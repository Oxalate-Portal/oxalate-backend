INSERT INTO user_roles VALUES (1, 1) ON CONFLICT (user_id, role_id) DO NOTHING;;
