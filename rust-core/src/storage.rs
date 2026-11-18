use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Mutex;

use chrono::NaiveDateTime;
use rusqlite::{params, Connection, OptionalExtension};
use serde::{Deserialize, Serialize};

use crate::schedule::{CurrentClassMatch, UnifiedSchedule};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct RecordingRecord {
    pub id: String,
    pub file_path: String,
    pub course_name: String,
    pub teacher: String,
    pub room: String,
    pub slot: String,
    pub started_at: i64,
    pub duration_ms: i64,
    pub status: String,
    pub notes: String,
    pub tags: Vec<String>,
}

pub struct Storage {
    conn: Mutex<Connection>,
    schedule_cache: Mutex<Option<Option<UnifiedSchedule>>>,
    data_version: AtomicU64,
}

impl Storage {
    pub fn open(db_path: &Path) -> Result<Self, String> {
        if let Some(parent) = db_path.parent() {
            std::fs::create_dir_all(parent)
                .map_err(|e| format!("Failed to create db dir: {e}"))?;
        }
        let conn = Connection::open(db_path)
            .map_err(|e| format!("Failed to open sqlite db: {e}"))?;
        let _ = conn.execute_batch(
            "PRAGMA journal_mode = WAL;
             PRAGMA synchronous = NORMAL;
             PRAGMA busy_timeout = 5000;",
        );
        Self::from_conn(conn)
    }

    #[cfg(test)]
    pub(crate) fn open_in_memory() -> Result<Self, String> {
        let conn = Connection::open_in_memory()
            .map_err(|e| format!("Failed to open in-memory sqlite db: {e}"))?;
        Self::from_conn(conn)
    }

    fn from_conn(conn: Connection) -> Result<Self, String> {
        conn.execute_batch(
            "
            CREATE TABLE IF NOT EXISTS schedules (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                source_url TEXT NOT NULL,
                json_data TEXT NOT NULL,
                synced_at INTEGER NOT NULL
            );
            CREATE TABLE IF NOT EXISTS recordings (
                id TEXT PRIMARY KEY,
                file_path TEXT NOT NULL,
                course_name TEXT NOT NULL,
                teacher TEXT NOT NULL DEFAULT '',
                room TEXT NOT NULL DEFAULT '',
                slot TEXT NOT NULL DEFAULT '',
                started_at INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'recording',
                notes TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT '[]'
            );
            CREATE INDEX IF NOT EXISTS idx_recordings_started_at
                ON recordings(started_at DESC);
            CREATE INDEX IF NOT EXISTS idx_recordings_course_started
                ON recordings(course_name, started_at DESC);
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );
            ",
        )
        .map_err(|e| format!("Failed to initialize sqlite schema: {e}"))?;

        Ok(Self {
            conn: Mutex::new(conn),
            schedule_cache: Mutex::new(None),
            data_version: AtomicU64::new(1),
        })
    }

    pub fn data_version(&self) -> u64 {
        self.data_version.load(Ordering::Acquire)
    }

    fn bump_version(&self) {
        self.data_version.fetch_add(1, Ordering::Release);
    }

    pub fn save_schedule(&self, schedule: &UnifiedSchedule) -> Result<(), String> {
        let json_data = serde_json::to_string(schedule)
            .map_err(|e| format!("Failed to serialize schedule: {e}"))?;
        {
            let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
            conn.execute(
                "INSERT INTO schedules (id, source_url, json_data, synced_at)
                 VALUES (1, ?1, ?2, ?3)
                 ON CONFLICT(id) DO UPDATE SET
                   source_url = excluded.source_url,
                   json_data = excluded.json_data,
                   synced_at = excluded.synced_at",
                params![schedule.source_url, json_data, schedule.synced_at],
            )
            .map_err(|e| format!("Failed to save schedule: {e}"))?;
        }
        if let Ok(mut cache) = self.schedule_cache.lock() {
            *cache = Some(Some(schedule.clone()));
        }
        self.bump_version();
        Ok(())
    }

    pub fn load_schedule(&self) -> Result<Option<UnifiedSchedule>, String> {
        if let Ok(cache) = self.schedule_cache.lock() {
            if let Some(ref cached) = *cache {
                return Ok(cached.clone());
            }
        }
        let parsed = {
            let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
            let raw: Option<String> = conn
                .query_row("SELECT json_data FROM schedules WHERE id = 1", [], |row| {
                    row.get(0)
                })
                .optional()
                .map_err(|e| format!("Failed to load schedule: {e}"))?;
            match raw {
                Some(s) => Some(
                    serde_json::from_str::<UnifiedSchedule>(&s)
                        .map_err(|e| format!("Failed to parse stored schedule: {e}"))?,
                ),
                None => None,
            }
        };
        if let Ok(mut cache) = self.schedule_cache.lock() {
            *cache = Some(parsed.clone());
        }
        Ok(parsed)
    }

    pub fn insert_recording(&self, rec: &RecordingRecord) -> Result<(), String> {
        let tags_json = serde_json::to_string(&rec.tags).unwrap_or_else(|_| "[]".to_string());
        {
            let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
            conn.execute(
                "INSERT INTO recordings (id, file_path, course_name, teacher, room, slot, started_at, duration_ms, status, notes, tags)
                 VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)",
                params![
                    rec.id,
                    rec.file_path,
                    rec.course_name,
                    rec.teacher,
                    rec.room,
                    rec.slot,
                    rec.started_at,
                    rec.duration_ms,
                    rec.status,
                    rec.notes,
                    tags_json
                ],
            )
            .map_err(|e| format!("Failed to insert recording: {e}"))?;
        }
        self.bump_version();
        Ok(())
    }

    pub fn complete_recording(&self, id: &str, duration_ms: i64) -> Result<RecordingRecord, String> {
        {
            let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
            let changed = conn
                .execute(
                    "UPDATE recordings SET status = 'completed', duration_ms = ?1 WHERE id = ?2",
                    params![duration_ms, id],
                )
                .map_err(|e| format!("Failed to complete recording: {e}"))?;
            if changed == 0 {
                return Err(format!("Recording not found: {id}"));
            }
        }
        self.bump_version();
        self.get_recording(id)?
            .ok_or_else(|| format!("Recording not found after update: {id}"))
    }

    pub fn get_recording(&self, id: &str) -> Result<Option<RecordingRecord>, String> {
        let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
        conn.query_row(
            "SELECT id, file_path, course_name, teacher, room, slot, started_at, duration_ms, status, notes, tags
             FROM recordings WHERE id = ?1",
            params![id],
            row_to_recording,
        )
        .optional()
        .map_err(|e| format!("Failed to query recording: {e}"))
    }

    pub fn list_recordings(
        &self,
        course_filter: Option<&str>,
        limit: usize,
    ) -> Result<Vec<RecordingRecord>, String> {
        let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
        let limit_i64 = limit.max(1) as i64;
        if let Some(course) = course_filter.map(str::trim).filter(|s| !s.is_empty()) {
            let mut stmt = conn
                .prepare_cached(
                    "SELECT id, file_path, course_name, teacher, room, slot, started_at, duration_ms, status, notes, tags
                     FROM recordings WHERE course_name = ?1 ORDER BY started_at DESC LIMIT ?2",
                )
                .map_err(|e| format!("Failed to prepare query: {e}"))?;
            collect_recording_rows(stmt.query_map(params![course, limit_i64], row_to_recording))
        } else {
            let mut stmt = conn
                .prepare_cached(
                    "SELECT id, file_path, course_name, teacher, room, slot, started_at, duration_ms, status, notes, tags
                     FROM recordings ORDER BY started_at DESC LIMIT ?1",
                )
                .map_err(|e| format!("Failed to prepare query: {e}"))?;
            collect_recording_rows(stmt.query_map(params![limit_i64], row_to_recording))
        }
    }

    pub fn update_recording_notes(
        &self,
        id: &str,
        notes: Option<&str>,
        tags: Option<Vec<String>>,
    ) -> Result<RecordingRecord, String> {
        let existing = self
            .get_recording(id)?
            .ok_or_else(|| format!("Recording not found: {id}"))?;
        let new_notes = notes.unwrap_or(&existing.notes).to_string();
        let new_tags = tags.unwrap_or(existing.tags);
        let tags_json = serde_json::to_string(&new_tags).unwrap_or_else(|_| "[]".to_string());

        {
            let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
            conn.execute(
                "UPDATE recordings SET notes = ?1, tags = ?2 WHERE id = ?3",
                params![new_notes, tags_json, id],
            )
            .map_err(|e| format!("Failed to update notes: {e}"))?;
        }
        self.bump_version();
        self.get_recording(id)?
            .ok_or_else(|| format!("Recording not found: {id}"))
    }

    pub fn delete_recording(&self, id: &str) -> Result<(), String> {
        let existing = self.get_recording(id)?;
        if let Some(rec) = existing {
            let _ = std::fs::remove_file(&rec.file_path);
        }
        {
            let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
            conn.execute("DELETE FROM recordings WHERE id = ?1", params![id])
                .map_err(|e| format!("Failed to delete recording: {e}"))?;
        }
        self.bump_version();
        Ok(())
    }

    pub fn get_setting(&self, key: &str) -> Result<Option<String>, String> {
        let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
        conn.query_row(
            "SELECT value FROM settings WHERE key = ?1",
            params![key],
            |row| row.get(0),
        )
        .optional()
        .map_err(|e| format!("Failed to get setting: {e}"))
    }

    pub fn set_setting(&self, key: &str, value: &str) -> Result<(), String> {
        {
            let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
            conn.execute(
                "INSERT INTO settings (key, value) VALUES (?1, ?2)
                 ON CONFLICT(key) DO UPDATE SET value = excluded.value",
                params![key, value],
            )
            .map_err(|e| format!("Failed to set setting: {e}"))?;
        }
        self.bump_version();
        Ok(())
    }

    pub fn count_course_recordings(&self, course_name: &str) -> Result<usize, String> {
        let conn = self.conn.lock().map_err(|_| "Storage lock poisoned")?;
        let count: i64 = conn
            .query_row(
                "SELECT COUNT(*) FROM recordings WHERE course_name = ?1",
                params![course_name],
                |row| row.get(0),
            )
            .map_err(|e| format!("Failed to count recordings: {e}"))?;
        Ok(count.max(0) as usize)
    }
}

fn collect_recording_rows(
    rows: rusqlite::Result<rusqlite::MappedRows<'_, impl FnMut(&rusqlite::Row<'_>) -> rusqlite::Result<RecordingRecord>>>,
) -> Result<Vec<RecordingRecord>, String> {
    let mapped = rows.map_err(|e| format!("Failed to list recordings: {e}"))?;
    mapped
        .map(|r| r.map_err(|e| format!("Row read error: {e}")))
        .collect()
}

fn row_to_recording(row: &rusqlite::Row<'_>) -> rusqlite::Result<RecordingRecord> {
    let tags_raw: String = row.get(10)?;
    let tags: Vec<String> = serde_json::from_str(&tags_raw).unwrap_or_default();
    Ok(RecordingRecord {
        id: row.get(0)?,
        file_path: row.get(1)?,
        course_name: row.get(2)?,
        teacher: row.get(3)?,
        room: row.get(4)?,
        slot: row.get(5)?,
        started_at: row.get(6)?,
        duration_ms: row.get(7)?,
        status: row.get(8)?,
        notes: row.get(9)?,
        tags,
    })
}

/// Replaces filesystem-unsafe characters so course/room names cannot break paths or traverse dirs.
pub fn sanitize_segment(s: &str) -> String {
    let cleaned: String = s
        .trim()
        .chars()
        .map(|c| match c {
            '/' | '\\' | ':' | '*' | '?' | '"' | '<' | '>' | '|' | '\0' => '_',
            _ if c.is_control() => '_',
            _ => c,
        })
        .collect();
    let trimmed = cleaned.trim_matches('.').trim();
    if trimmed.is_empty() {
        "Uncategorized".to_string()
    } else {
        trimmed.to_string()
    }
}

#[cfg(test)]
pub fn build_recording_path(
    base_dir: &Path,
    course: Option<&CurrentClassMatch>,
    dt: NaiveDateTime,
) -> PathBuf {
    build_recording_path_with_template(base_dir, course, dt, None, 1)
}

/// Formats a NaiveDateTime according to a named/pattern date format.
pub fn format_date_with_pattern(dt: NaiveDateTime, date_format: Option<&str>) -> String {
    match date_format.map(str::trim).unwrap_or("yyyy-MM-dd") {
        "yyyyMMdd" => dt.format("%Y%m%d").to_string(),
        "MM-dd" => dt.format("%m-%d").to_string(),
        "dd-MM-yyyy" => dt.format("%d-%m-%Y").to_string(),
        "yyyy.MM.dd" => dt.format("%Y.%m.%d").to_string(),
        "MM.dd" => dt.format("%m.%d").to_string(),
        "MMM dd" => dt.format("%b %d").to_string(),
        "yyyy-MM-dd EEE" => dt.format("%Y-%m-%d %a").to_string(),
        _ => dt.format("%Y-%m-%d").to_string(),
    }
}

#[cfg(test)]
pub fn build_recording_path_with_template(
    base_dir: &Path,
    course: Option<&CurrentClassMatch>,
    dt: NaiveDateTime,
    template: Option<&str>,
    lesson_number: usize,
) -> PathBuf {
    build_recording_path_with_options(base_dir, course, dt, template, None, lesson_number)
}

pub fn build_recording_path_with_options(
    base_dir: &Path,
    course: Option<&CurrentClassMatch>,
    dt: NaiveDateTime,
    template: Option<&str>,
    date_format: Option<&str>,
    lesson_number: usize,
) -> PathBuf {
    let date_str = format_date_with_pattern(dt, date_format);
    let time_str = dt.format("%H%M").to_string();
    let year_str = dt.format("%Y").to_string();
    let month_str = dt.format("%m").to_string();
    let day_str = dt.format("%d").to_string();
    let weekday_str = dt.format("%a").to_string();

    let (folder, class_name, room, teacher, slot) = match course {
        Some(m) if !m.course.name.trim().is_empty() => (
            sanitize_segment(&m.course.name),
            m.course.name.trim().to_string(),
            m.course.room.trim().to_string(),
            m.course.teacher.trim().to_string(),
            m.slot.trim().to_string(),
        ),
        _ => (
            "Uncategorized".to_string(),
            "Uncategorized".to_string(),
            String::new(),
            String::new(),
            String::new(),
        ),
    };

    let custom = template.map(str::trim).filter(|s| !s.is_empty());
    let stem = if let Some(tmpl) = custom {
        let mut out = tmpl.to_string();
        for key in ["{class}", "{course}"] {
            out = out.replace(key, &class_name);
        }
        out = out.replace("{date}", &date_str);
        out = out.replace("{year}", &year_str);
        out = out.replace("{month}", &month_str);
        out = out.replace("{day}", &day_str);
        out = out.replace("{weekday}", &weekday_str);
        out = out.replace("{time}", &time_str);
        out = out.replace("{room}", &room);
        out = out.replace("{teacher}", &teacher);
        out = out.replace("{slot}", &slot);
        let lesson_str = lesson_number.max(1).to_string();
        for key in [
            "{lesson number}",
            "{lesson_number}",
            "{lesson}",
            "{lession number}",
            "{lession_number}",
            "{lession}",
        ] {
            out = out.replace(key, &lesson_str);
        }
        let cleaned = sanitize_segment(&out);
        if cleaned.is_empty() || (cleaned == "Uncategorized" && class_name != "Uncategorized") {
            format!("{date_str}_{time_str}_{folder}")
        } else {
            cleaned
        }
    } else if class_name != "Uncategorized" {
        let room_seg = sanitize_segment(&room);
        if room.is_empty() {
            format!("{date_str}_{time_str}_{folder}")
        } else {
            format!("{date_str}_{time_str}_{folder}_{room_seg}")
        }
    } else {
        format!("{date_str}_{time_str}_Recording")
    };

    base_dir.join(folder).join(format!("{stem}.m4a"))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::schedule::{CourseSlot, MatchKind, TimeSlotRow};
    use chrono::NaiveDate;

    #[test]
    fn test_sanitize_and_build_path() {
        let dt = NaiveDate::from_ymd_opt(2026, 11, 13)
            .unwrap()
            .and_hms_opt(8, 10, 0)
            .unwrap();
        let m = CurrentClassMatch {
            kind: MatchKind::Active,
            slot: "1".to_string(),
            time: "08:10~09:00".to_string(),
            weekday_index: 4,
            course: CourseSlot {
                name: "C++/Web: Lab".to_string(),
                teacher: "王".to_string(),
                room: "33/09".to_string(),
                color: "#00DAC3".to_string(),
            },
        };
        let p = build_recording_path(Path::new("/recordings"), Some(&m), dt);
        assert_eq!(
            p,
            PathBuf::from("/recordings/C++_Web_ Lab/2026-11-13_0810_C++_Web_ Lab_33_09.m4a")
        );

        let p_none = build_recording_path(Path::new("/recordings"), None, dt);
        assert_eq!(
            p_none,
            PathBuf::from("/recordings/Uncategorized/2026-11-13_0810_Recording.m4a")
        );
    }

    #[test]
    fn test_custom_filename_template() {
        let dt = NaiveDate::from_ymd_opt(2026, 11, 13)
            .unwrap()
            .and_hms_opt(19, 30, 0)
            .unwrap();
        let m = CurrentClassMatch {
            kind: MatchKind::Active,
            slot: "3".to_string(),
            time: "10:10~11:00".to_string(),
            weekday_index: 4,
            course: CourseSlot {
                name: "大學之道".to_string(),
                teacher: "何昕家".to_string(),
                room: "3309".to_string(),
                color: "#00DAC3".to_string(),
            },
        };
        let p = build_recording_path_with_template(
            Path::new("/recordings"),
            Some(&m),
            dt,
            Some("{class} {date} - {lession number}"),
            3,
        );
        assert_eq!(
            p,
            PathBuf::from("/recordings/大學之道/大學之道 2026-11-13 - 3.m4a")
        );

        let p_fmt = build_recording_path_with_options(
            Path::new("/recordings"),
            Some(&m),
            dt,
            Some("{class} {date} - {lesson number}"),
            Some("MMM dd"),
            2,
        );
        assert_eq!(
            p_fmt,
            PathBuf::from("/recordings/大學之道/大學之道 Nov 13 - 2.m4a")
        );
    }

    #[test]
    fn test_sqlite_recording_lifecycle_and_notes() {
        let storage = Storage::open_in_memory().unwrap();
        let v0 = storage.data_version();
        let rec = RecordingRecord {
            id: "rec-1".to_string(),
            file_path: "/tmp/nonexistent.m4a".to_string(),
            course_name: "大學之道".to_string(),
            teacher: "何昕家".to_string(),
            room: "3309".to_string(),
            slot: "1".to_string(),
            started_at: 1700000000,
            duration_ms: 0,
            status: "recording".to_string(),
            notes: String::new(),
            tags: vec![],
        };
        storage.insert_recording(&rec).unwrap();
        assert!(storage.data_version() > v0);
        let completed = storage.complete_recording("rec-1", 12000).unwrap();
        assert_eq!(completed.status, "completed");
        assert_eq!(completed.duration_ms, 12000);

        let updated = storage
            .update_recording_notes(
                "rec-1",
                Some("Discussed midterm project"),
                Some(vec!["midterm".to_string(), "summary".to_string()]),
            )
            .unwrap();
        assert_eq!(updated.notes, "Discussed midterm project");
        assert_eq!(updated.tags, vec!["midterm", "summary"]);

        let list = storage.list_recordings(Some("大學之道"), 10).unwrap();
        assert_eq!(list.len(), 1);
        assert_eq!(list[0].id, "rec-1");
    }

    #[test]
    fn test_sqlite_schedule_roundtrip() {
        let storage = Storage::open_in_memory().unwrap();
        let sched = UnifiedSchedule {
            school_name: "國立臺中科技大學".to_string(),
            source_url: "https://kewutong.cloud-service.win/api/v2/share/qVaaFS5A".to_string(),
            synced_at: 1700000000,
            slots: vec![TimeSlotRow {
                slot: "1".to_string(),
                time: "08:10~09:00".to_string(),
                days: vec![None, None, None, None, None, None, None],
            }],
        };
        storage.save_schedule(&sched).unwrap();
        let loaded = storage.load_schedule().unwrap().unwrap();
        assert_eq!(loaded, sched);
    }
}
