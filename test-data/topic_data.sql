-- Thread 1: Best Scuba Diving Locations for Beginners
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('Best Scuba Diving Locations for Beginners',
        'Let’s discuss some of the easiest and safest diving locations for beginners. Any suggestions?',
        2, -- User initiating the discussion
        (SELECT id FROM comments WHERE title = 'Forum root topic'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

-- Responses to Thread 1
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Best Scuba Diving Locations for Beginners',
        'I think the Great Barrier Reef in Australia is fantastic for beginners. It’s shallow in many areas and absolutely stunning!',
        3,
        (SELECT id FROM comments WHERE title = 'Best Scuba Diving Locations for Beginners'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Best Scuba Diving Locations for Beginners',
        'I agree with the Great Barrier Reef! I’d also recommend Cozumel in Mexico. Clear waters and calm currents make it ideal for beginners.',
        4,
        (SELECT id FROM comments WHERE title = 'Best Scuba Diving Locations for Beginners'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Best Scuba Diving Locations for Beginners',
        'I tried Cozumel on my first dive, and it was magical! Can anyone recommend beginner-friendly diving gear?',
        5,
        (SELECT id FROM comments WHERE body = 'I agree with the Great Barrier Reef! I’d also recommend Cozumel in Mexico. Clear waters and calm currents make it ideal for beginners.'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

-- Thread 2: Diving Safety Tips Everyone Should Know
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('Diving Safety Tips Everyone Should Know',
        'What are your top safety tips for scuba diving? Let’s help new divers stay safe out there.',
        6,
        (SELECT id FROM comments WHERE title = 'Forum root topic'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

-- Responses to Thread 2
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Diving Safety Tips Everyone Should Know',
        'Always double-check your equipment before entering the water. A faulty regulator is no joke.',
        7,
        (SELECT id FROM comments WHERE title = 'Diving Safety Tips Everyone Should Know'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Diving Safety Tips Everyone Should Know',
        'Great point! I’d add that you should never dive alone. Always have a buddy to watch your back.',
        8,
        (SELECT id FROM comments WHERE title = 'Diving Safety Tips Everyone Should Know'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Diving Safety Tips Everyone Should Know',
        'What’s the best way to practice buoyancy control? I always seem to struggle with staying level.',
        9,
        (SELECT id FROM comments WHERE body = 'Great point! I’d add that you should never dive alone. Always have a buddy to watch your back.'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

-- Thread 3: What’s in Your Dive Bag?
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('What’s in Your Dive Bag?',
        'What essentials do you always bring with you on a dive? Let’s compare gear!',
        10,
        (SELECT id FROM comments WHERE title = 'Forum root topic'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

-- Responses to Thread 3
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: What’s in Your Dive Bag?',
        'I always pack a spare mask, just in case. It’s saved me more than once!',
        11,
        (SELECT id FROM comments WHERE title = 'What’s in Your Dive Bag?'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: What’s in Your Dive Bag?',
        'Same here! I also bring a dive torch, even for daytime dives. It’s great for peeking into crevices.',
        12,
        (SELECT id FROM comments WHERE title = 'What’s in Your Dive Bag?'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: What’s in Your Dive Bag?',
        'Do you use a specific dive torch? I’ve been thinking about upgrading mine.',
        13,
        (SELECT id FROM comments WHERE body = 'Same here! I also bring a dive torch, even for daytime dives. It’s great for peeking into crevices.'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

-- Thread 4: Best Underwater Photography Tips
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('Best Underwater Photography Tips',
        'Underwater photography can be tricky. Any tips for getting the perfect shot?',
        14,
        (SELECT id FROM comments WHERE title = 'Forum root topic'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

-- Responses to Thread 4
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Best Underwater Photography Tips',
        'Lighting is everything! Always try to use natural light whenever possible.',
        15,
        (SELECT id FROM comments WHERE title = 'Best Underwater Photography Tips'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Best Underwater Photography Tips',
        'I’d add that getting close to your subject is key. Water reduces clarity over distance.',
        16,
        (SELECT id FROM comments WHERE title = 'Best Underwater Photography Tips'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Best Underwater Photography Tips',
        'Do you use any specific filters for underwater shots? My reds never look quite right.',
        17,
        (SELECT id FROM comments WHERE body = 'I’d add that getting close to your subject is key. Water reduces clarity over distance.'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

-- Thread 5: Favorite Marine Life Encounters
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('Favorite Marine Life Encounters',
        'What’s the most incredible marine life encounter you’ve had while diving?',
        18,
        (SELECT id FROM comments WHERE title = 'Forum root topic'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

-- Responses to Thread 5
INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Favorite Marine Life Encounters',
        'Swimming alongside a whale shark was a life-changing experience for me!',
        19,
        (SELECT id FROM comments WHERE title = 'Favorite Marine Life Encounters'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Favorite Marine Life Encounters',
        'That sounds incredible! I saw a manta ray doing barrel rolls, and it felt like it was dancing for us.',
        20,
        (SELECT id FROM comments WHERE title = 'Favorite Marine Life Encounters'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);

INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason)
VALUES ('RE: Favorite Marine Life Encounters',
        'I once encountered a pod of dolphins—they were so curious and playful. Truly unforgettable!',
        21,
        (SELECT id FROM comments WHERE title = 'Favorite Marine Life Encounters'),
        'USER_COMMENT',
        'PUBLISHED',
        NULL);
