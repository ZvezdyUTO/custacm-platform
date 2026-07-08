insert into ods_atcoder__problem (
    problem_id,
    contest_id,
    problem_index,
    problem_name,
    title,
    batch_id,
    fetched_at,
    raw_payload,
    payload_hash
) values (
    :problemId,
    :contestId,
    :problemIndex,
    :problemName,
    :title,
    :batchId,
    :fetchedAt,
    :rawPayload,
    :payloadHash
)
on duplicate key update
    contest_id = values(contest_id),
    problem_index = values(problem_index),
    problem_name = values(problem_name),
    title = values(title),
    batch_id = values(batch_id),
    fetched_at = values(fetched_at),
    raw_payload = values(raw_payload),
    payload_hash = values(payload_hash),
    updated_at = current_timestamp(6);
