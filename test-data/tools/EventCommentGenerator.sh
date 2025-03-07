#!/bin/bash

# Default values
NUM_THREADS=5
THREAD_DEPTH=10

# Parse command line options
while getopts "t:d:" opt; do
  case ${opt} in
    t ) NUM_THREADS=$OPTARG ;;
    d ) THREAD_DEPTH=$OPTARG ;;
    \? ) echo "Usage: $0 [-t num_threads] [-d thread_depth]" >&2; exit 1 ;;
  esac
done

# Generate SQL script
echo "-- Auto-generated SQL for comments with threading" > generate_comments.sql
NOWDATETIME=$(date +"%Y-%m-%d %H:%M:%S")
# Loop to generate main threads
for i in $(seq 1 $NUM_THREADS); do
  THREAD_TITLE="Diving Discussion Thread #${i} ${NOWDATETIME}"
  echo "INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason, created_at, modified_at) VALUES" >> generate_comments.sql
  echo "('${THREAD_TITLE}', 'Let’s talk about dive event #${i}.', 1, 1, 'TOPIC', 'PUBLISHED', NULL, NOW(), NULL);" >> generate_comments.sql
  echo "INSERT INTO event_comments VALUES (DEFAULT, ${i}, (SELECT id FROM comments WHERE title = '${THREAD_TITLE}'));" >> generate_comments.sql
  # Loop to generate replies in a chain
  PARENT_TITLE="${THREAD_TITLE}"
  for j in $(seq 1 ${THREAD_DEPTH}); do
    COMMENT_TITLE="Re: ${THREAD_TITLE} - Reply ${j}"
    echo "INSERT INTO comments (title, body, user_id, parent_comment_id, comment_type, comment_status, cancel_reason, created_at, modified_at) VALUES" >> generate_comments.sql
    echo "('${COMMENT_TITLE}', 'This is reply #${j} for dive event #${i}.', $(( (j % 5) + 1 )), (SELECT id FROM comments WHERE title = '${PARENT_TITLE}'), 'USER_COMMENT', 'PUBLISHED', NULL, NOW(), NULL);" >> generate_comments.sql

    # Update parent for next reply in the chain
    PARENT_TITLE="${COMMENT_TITLE}"
  done
  echo "-- ************* Thread ${i} done" >> generate_comments.sql
done

echo "-- SQL generation complete. Run the script in your database." >> generate_comments.sql
echo "SQL script generated as 'generate_comments.sql'."
