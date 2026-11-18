-- 1. Users
INSERT INTO users (id, username, email, full_name, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'alex_lead', 'alex.lead@bugtracker.com', 'Alex Lead', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222222', 'anna_qa', 'anna.qa@bugtracker.com', 'Anna Tester', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333', 'john_dev', 'john.dev@bugtracker.com', 'John Developer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('44444444-4444-4444-4444-444444444444', 'maria_obs', 'maria.obs@bugtracker.com', 'Maria Observer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. Projects
INSERT INTO projects (id, name, description, git_repo_url, created_by, is_active, tracking_active, created_at, updated_at) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Bug Tracker Core', 'Main system development project', 'https://github.com/team1/bugtracker', '11111111-1111-1111-1111-111111111111', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Unstaffed Mobile App', 'Mobile client project without QA team yet', 'https://github.com/team1/mobile-app', '11111111-1111-1111-1111-111111111111', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Project Members
INSERT INTO project_members (id, project_id, user_id, role, joined_at) VALUES
('11111111-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'TEAM_LEAD', CURRENT_TIMESTAMP),
('11111111-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '22222222-2222-2222-2222-222222222222', 'TESTER', CURRENT_TIMESTAMP),
('11111111-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-3333-3333-3333-333333333333', 'DEVELOPER', CURRENT_TIMESTAMP),
('11111111-0000-0000-0000-000000000004', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '44444444-4444-4444-4444-444444444444', 'OBSERVER', CURRENT_TIMESTAMP),
('22222222-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'TEAM_LEAD', CURRENT_TIMESTAMP);

-- 4. Components
INSERT INTO components (id, project_id, name, description) VALUES
('cccccccc-cccc-cccc-cccc-cccccccc0001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Backend API', 'Server side REST API'),
('cccccccc-cccc-cccc-cccc-cccccccc0002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Database & Migrations', 'PostgreSQL and Flyway schema');

-- 5. Releases
INSERT INTO releases (id, project_id, version_name, release_date, is_released) VALUES
('dddddddd-dddd-dddd-dddd-dddddddd0001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'v1.0.0', '2026-12-01', false),
('dddddddd-dddd-dddd-dddd-dddddddd0002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'v0.9.0', '2026-11-15', true);

-- 6. Tags
INSERT INTO tags (id, project_id, name, color) VALUES
('eeeeeeee-eeee-eeee-eeee-eeeeeeee0001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Backend', '#3498DB'),
('eeeeeeee-eeee-eeee-eeee-eeeeeeee0002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Security', '#E74C3C');

-- 7. Issues
INSERT INTO issues (id, project_id, title, description, steps_to_reproduce, priority, status, resolution, reporter_id, assignee_id, component_id, release_id, fix_release_id, deadline, created_at, updated_at) VALUES
('10000000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'NullPointerException on invalid auth token', 'Server throws 500 when bearer token is malformed', '1. Send GET with invalid Bearer token
2. Observe 500 status', 'CRITICAL', 'NEW', null, '22222222-2222-2222-2222-222222222222', null, 'cccccccc-cccc-cccc-cccc-cccccccc0001', 'dddddddd-dddd-dddd-dddd-dddddddd0001', null, '2026-11-25', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('10000000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Project members list pagination missing', 'API returns unpaged list for large teams', '1. Add 50 members
2. Query list', 'MEDIUM', 'IN_PROGRESS', null, '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'cccccccc-cccc-cccc-cccc-cccccccc0001', 'dddddddd-dddd-dddd-dddd-dddddddd0001', null, '2026-11-28', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('10000000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Fix role validation for team lead removal', 'Prevented removing the only team lead from project team', '1. Try to delete the only team lead
2. Should receive 422', 'HIGH', 'IN_REVIEW', null, '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'cccccccc-cccc-cccc-cccc-cccccccc0001', 'dddddddd-dddd-dddd-dddd-dddddddd0001', null, '2026-11-22', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('10000000-0000-0000-0000-000000000004', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Missing unique constraint on project name', 'Duplicate project names caused conflict in routing', 'Fixed and verified in v0.9.0', 'HIGH', 'CLOSED', 'FIXED', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'cccccccc-cccc-cccc-cccc-cccccccc0002', 'dddddddd-dddd-dddd-dddd-dddddddd0002', 'dddddddd-dddd-dddd-dddd-dddddddd0002', '2026-11-14', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 8. Issue Tags
INSERT INTO issue_tags (issue_id, tag_id) VALUES
('10000000-0000-0000-0000-000000000001', 'eeeeeeee-eeee-eeee-eeee-eeeeeeee0001'),
('10000000-0000-0000-0000-000000000001', 'eeeeeeee-eeee-eeee-eeee-eeeeeeee0002');

-- 9. Issue Links
INSERT INTO issue_links (id, source_issue_id, target_issue_id, link_type) VALUES
('ffffffff-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003', 'RELATES_TO');
