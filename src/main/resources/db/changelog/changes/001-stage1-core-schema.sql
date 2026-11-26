-- Stage 1 core schema: users, projects, bugs, collaboration and audit tables.

create table users (
    can_create_projects boolean not null,
    email_verified boolean not null,
    is_admin boolean not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    id uuid not null,
    status varchar(32) not null check (status in ('ACTIVE', 'BLOCKED', 'DEACTIVATED')),
    avatar_url varchar(1024),
    email varchar(255) not null unique,
    full_name varchar(255) not null,
    password_hash varchar(255) not null,
    primary key (id)
);

create table notification_settings (
    email_enabled boolean not null,
    in_app_enabled boolean not null,
    updated_at timestamp(6) with time zone not null,
    id uuid not null,
    user_id uuid not null unique,
    events_json text not null,
    primary key (id)
);

create table refresh_tokens (
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    revoked_at timestamp(6) with time zone,
    id uuid not null,
    user_id uuid not null,
    token_hash varchar(128) not null unique,
    primary key (id)
);

create table email_verification_tokens (
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    used_at timestamp(6) with time zone,
    id uuid not null,
    user_id uuid not null,
    token_hash varchar(128) not null unique,
    primary key (id)
);

create table password_reset_tokens (
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    used_at timestamp(6) with time zone,
    id uuid not null,
    user_id uuid not null,
    token_hash varchar(128) not null unique,
    primary key (id)
);

create table audit_logs (
    created_at timestamp(6) with time zone not null,
    actor_id uuid,
    id uuid not null,
    target_id uuid,
    action varchar(100) not null,
    target_type varchar(100) not null,
    details_json text,
    primary key (id)
);

create table projects (
    bug_tracking_enabled boolean not null,
    end_date date,
    start_date date,
    bug_sequence bigint not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    id uuid not null,
    owner_id uuid not null,
    status varchar(32) not null check (status in ('ACTIVE', 'ARCHIVED')),
    name varchar(150) not null unique,
    tech_stack varchar(500),
    description text,
    primary key (id)
);

create table project_members (
    joined_at timestamp(6) with time zone not null,
    id uuid not null,
    project_id uuid not null,
    user_id uuid not null,
    role varchar(32) not null check (role in ('TEAM_LEAD', 'DEVELOPER', 'TESTER', 'OBSERVER')),
    status varchar(32) not null check (status in ('ACTIVE')),
    primary key (id),
    constraint uk_project_member unique (project_id, user_id)
);

create table project_invitations (
    created_at timestamp(6) with time zone not null,
    expires_at timestamp(6) with time zone not null,
    id uuid not null,
    invited_by_id uuid not null,
    project_id uuid not null,
    role varchar(32) not null check (role in ('TEAM_LEAD', 'DEVELOPER', 'TESTER', 'OBSERVER')),
    status varchar(32) not null check (status in ('PENDING', 'ACCEPTED', 'CANCELLED', 'EXPIRED')),
    token_hash varchar(128) not null unique,
    email varchar(255) not null,
    primary key (id)
);

create table software_components (
    created_at timestamp(6) with time zone not null,
    id uuid not null,
    owner_developer_id uuid,
    project_id uuid not null,
    name varchar(150) not null,
    description text,
    primary key (id),
    constraint uk_component_project_name unique (project_id, name)
);

create table labels (
    created_at timestamp(6) with time zone not null,
    id uuid not null,
    project_id uuid not null,
    color varchar(32),
    name varchar(100) not null,
    primary key (id),
    constraint uk_label_project_name unique (project_id, name)
);

create table bugs (
    closed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone not null,
    deadline timestamp(6) with time zone,
    updated_at timestamp(6) with time zone not null,
    assignee_id uuid,
    author_id uuid not null,
    component_id uuid,
    id uuid not null,
    project_id uuid not null,
    bug_key varchar(32) not null unique,
    priority varchar(32) not null check (priority in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    recommended_priority varchar(32) not null check (recommended_priority in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    resolution varchar(32) check (resolution in ('FIXED', 'NOT_A_BUG', 'DUPLICATE', 'CANNOT_REPRODUCE')),
    status varchar(32) not null check (status in ('NEW', 'PLANNED', 'IN_PROGRESS', 'NEEDS_INFO', 'IN_REVIEW', 'RETURNED', 'CLOSED', 'REOPENED')),
    actual_result text,
    description text not null,
    environment text not null,
    expected_result text,
    steps_to_reproduce text not null,
    title varchar(255) not null,
    primary key (id)
);

create table bug_labels (
    bug_id uuid not null,
    label_id uuid not null,
    primary key (bug_id, label_id)
);

create table fix_plans (
    deadline timestamp(6) with time zone,
    updated_at timestamp(6) with time zone not null,
    bug_id uuid not null unique,
    id uuid not null,
    planned_by_id uuid not null,
    priority varchar(32) not null check (priority in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    priority_change_reason text,
    rationale text,
    primary key (id)
);

create table comments (
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    author_id uuid not null,
    bug_id uuid not null,
    id uuid not null,
    body text not null,
    primary key (id)
);

create table attachments (
    created_at timestamp(6) with time zone not null,
    size_bytes bigint not null,
    bug_id uuid not null,
    comment_id uuid,
    id uuid not null,
    uploaded_by_id uuid not null,
    s3_object_key varchar(512) not null unique,
    content_type varchar(255) not null,
    file_name varchar(255) not null,
    primary key (id)
);

create table bug_links (
    created_at timestamp(6) with time zone not null,
    created_by_id uuid not null,
    id uuid not null,
    source_bug_id uuid not null,
    target_bug_id uuid not null,
    link_type varchar(32) not null check (link_type in ('DUPLICATES', 'BLOCKS', 'RELATES_TO')),
    primary key (id),
    constraint uk_bug_link unique (source_bug_id, target_bug_id, link_type)
);

create table bug_history (
    created_at timestamp(6) with time zone not null,
    actor_id uuid not null,
    bug_id uuid not null,
    id uuid not null,
    field_name varchar(100) not null,
    new_value text,
    old_value text,
    primary key (id)
);

create index idx_attachments_bug on attachments (bug_id);
create index idx_audit_logs_created_at on audit_logs (created_at);
create index idx_audit_logs_actor on audit_logs (actor_id);
create index idx_bug_history_bug on bug_history (bug_id);
create index idx_bugs_project_status on bugs (project_id, status);
create index idx_bugs_assignee on bugs (assignee_id);
create index idx_comments_bug on comments (bug_id);
create index idx_project_invitations_email on project_invitations (email);
create index idx_refresh_tokens_user on refresh_tokens (user_id);

alter table notification_settings
    add constraint fk_notification_settings_user foreign key (user_id) references users (id) on delete cascade;

alter table refresh_tokens
    add constraint fk_refresh_tokens_user foreign key (user_id) references users (id) on delete cascade;

alter table email_verification_tokens
    add constraint fk_email_verification_tokens_user foreign key (user_id) references users (id) on delete cascade;

alter table password_reset_tokens
    add constraint fk_password_reset_tokens_user foreign key (user_id) references users (id) on delete cascade;

alter table audit_logs
    add constraint fk_audit_logs_actor foreign key (actor_id) references users (id) on delete cascade;

alter table projects
    add constraint fk_projects_owner foreign key (owner_id) references users (id) on delete cascade;

alter table project_members
    add constraint fk_project_members_project foreign key (project_id) references projects (id) on delete cascade;

alter table project_members
    add constraint fk_project_members_user foreign key (user_id) references users (id) on delete cascade;

alter table project_invitations
    add constraint fk_project_invitations_project foreign key (project_id) references projects (id) on delete cascade;

alter table project_invitations
    add constraint fk_project_invitations_invited_by foreign key (invited_by_id) references users (id) on delete cascade;

alter table software_components
    add constraint fk_software_components_project foreign key (project_id) references projects (id) on delete cascade;

alter table software_components
    add constraint fk_software_components_owner_developer foreign key (owner_developer_id) references users (id) on delete cascade;

alter table labels
    add constraint fk_labels_project foreign key (project_id) references projects (id) on delete cascade;

alter table bugs
    add constraint fk_bugs_project foreign key (project_id) references projects (id) on delete cascade;

alter table bugs
    add constraint fk_bugs_author foreign key (author_id) references users (id) on delete cascade;

alter table bugs
    add constraint fk_bugs_assignee foreign key (assignee_id) references users (id) on delete cascade;

alter table bugs
    add constraint fk_bugs_component foreign key (component_id) references software_components (id) on delete cascade;

alter table bug_labels
    add constraint fk_bug_labels_bug foreign key (bug_id) references bugs (id) on delete cascade;

alter table bug_labels
    add constraint fk_bug_labels_label foreign key (label_id) references labels (id) on delete cascade;

alter table fix_plans
    add constraint fk_fix_plans_bug foreign key (bug_id) references bugs (id) on delete cascade;

alter table fix_plans
    add constraint fk_fix_plans_planned_by foreign key (planned_by_id) references users (id) on delete cascade;

alter table comments
    add constraint fk_comments_bug foreign key (bug_id) references bugs (id) on delete cascade;

alter table comments
    add constraint fk_comments_author foreign key (author_id) references users (id) on delete cascade;

alter table attachments
    add constraint fk_attachments_bug foreign key (bug_id) references bugs (id) on delete cascade;

alter table attachments
    add constraint fk_attachments_comment foreign key (comment_id) references comments (id) on delete set null;

alter table attachments
    add constraint fk_attachments_uploaded_by foreign key (uploaded_by_id) references users (id) on delete cascade;

alter table bug_links
    add constraint fk_bug_links_source_bug foreign key (source_bug_id) references bugs (id) on delete cascade;

alter table bug_links
    add constraint fk_bug_links_target_bug foreign key (target_bug_id) references bugs (id) on delete cascade;

alter table bug_links
    add constraint fk_bug_links_created_by foreign key (created_by_id) references users (id) on delete cascade;

alter table bug_history
    add constraint fk_bug_history_bug foreign key (bug_id) references bugs (id) on delete cascade;

alter table bug_history
    add constraint fk_bug_history_actor foreign key (actor_id) references users (id) on delete cascade;
