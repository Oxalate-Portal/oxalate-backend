-- Create tag groups table
CREATE TABLE tag_groups (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    tag_type VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create tag group translations table
CREATE TABLE tag_group_translations (
    id BIGSERIAL PRIMARY KEY,
    tag_group_id BIGINT NOT NULL REFERENCES tag_groups(id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL,
    name VARCHAR(100) NOT NULL,
    UNIQUE (tag_group_id, language)
);

-- Create tags table
CREATE TABLE tags (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    tag_group_id BIGINT REFERENCES tag_groups(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create tag translations table
CREATE TABLE tag_translations (
    id BIGSERIAL PRIMARY KEY,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    language VARCHAR(2) NOT NULL,
    name VARCHAR(100) NOT NULL,
    UNIQUE (tag_id, language)
);

-- Create junction table for user-tag associations
CREATE TABLE user_tags (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, tag_id)
);

-- Create junction table for event-tag associations
CREATE TABLE event_tags (
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, tag_id)
);

-- Add indexes for better performance
CREATE INDEX idx_tag_group_id ON tags(tag_group_id);
CREATE INDEX idx_tag_translations_tag_id ON tag_translations(tag_id);
CREATE INDEX idx_tag_group_translations_tag_group_id ON tag_group_translations(tag_group_id);
CREATE INDEX idx_user_tags_tag_id ON user_tags(tag_id);
CREATE INDEX idx_event_tags_tag_id ON event_tags(tag_id);
