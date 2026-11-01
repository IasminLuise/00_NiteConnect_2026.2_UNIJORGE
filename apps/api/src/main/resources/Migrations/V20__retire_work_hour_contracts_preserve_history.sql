-- The prior work-hour and justification contracts are retired. Their tables and
-- rows remain intact until a separately approved data inventory and migration.
COMMENT ON TABLE nite_connect.calendar_events IS 'Active B04 agenda and meeting sessions; retained for QR attendance.';
COMMENT ON TABLE nite_connect.event_participants IS 'Active B04 expected meeting participants; retained for agenda and future attendance.';
COMMENT ON TABLE nite_connect.attendance_records IS 'Active B04 meeting presence records; retained as the historical attendance source.';
COMMENT ON TABLE nite_connect.participation_periods IS 'Mixed historical table: meeting presence segments and retired work-log rows. Preserve rows and schema pending an approved data strategy.';
COMMENT ON TABLE nite_connect.participation_events IS 'Append-only B04 attendance and legacy participation audit history; preserve.';
COMMENT ON TABLE nite_connect.absence_justifications IS 'Retired B04 absence-justification records; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.correction_requests IS 'Retired B04 correction requests; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.participation_calendar_versions IS 'Retired B04 weekly calendar exceptions; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.participation_adjustments IS 'Retired B04 replacement and dispensation requests; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.participation_adjustment_events IS 'Retired B04 replacement and dispensation audit events; preserve.';
COMMENT ON TABLE nite_connect.participation_replacement_allocations IS 'Retired B04 replacement allocations; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.participation_review_batches IS 'Retired B04 work-log review batches; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.participation_review_batch_items IS 'Retired B04 work-log review batch decisions; preserve.';
COMMENT ON TABLE nite_connect.participation_load_policy_versions IS 'Retired B04 weekly work-hour policy versions; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.participation_load_policy_drafts IS 'Retired B04 weekly work-hour policy drafts; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.participation_load_policy_events IS 'Retired B04 weekly work-hour policy audit events; preserve.';
COMMENT ON TABLE nite_connect.work_log_correction_responses IS 'Retired B04 remote work-log correction responses; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.work_log_correction_decisions IS 'Retired B04 remote work-log correction decisions; preserve.';
COMMENT ON TABLE nite_connect.justification_attachment_retention_policies IS 'Retired B04 justification attachment retention policy records; preserve pending institutional retention and storage review.';
COMMENT ON TABLE nite_connect.justification_attachment_retention_events IS 'Retired B04 justification attachment retention audit history; preserve.';
COMMENT ON TABLE nite_connect.absence_justification_retention_classifications IS 'Retired B04 justification retention classifications; preserve pending institutional retention and data review.';
COMMENT ON TABLE nite_connect.absence_justification_attachments IS 'Retired B04 private justification attachment metadata; do not delete metadata or stored objects without an approved inventory and retention decision.';
COMMENT ON TABLE nite_connect.absence_justification_attachment_events IS 'Retired B04 private attachment audit history; preserve.';
COMMENT ON TABLE nite_connect.absence_justification_attachment_retention_holds IS 'Retired B04 attachment retention holds; preserve pending institutional retention and storage review.';
COMMENT ON TABLE nite_connect.justification_attachment_retention_purge_runs IS 'Retired B04 attachment purge run records; preserve as operational history.';
COMMENT ON TABLE nite_connect.justification_attachment_retention_purge_run_events IS 'Retired B04 attachment purge audit events; preserve.';
COMMENT ON TABLE nite_connect.justification_attachment_retention_purge_items IS 'Retired B04 attachment purge item records; preserve as operational history.';
