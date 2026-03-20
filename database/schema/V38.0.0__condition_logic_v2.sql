-- V38: Upgrade condition_logic to TEXT and migrate V1 → V2 format
-- V2 supports AND/OR nested conditions + multiple actions

-- 1. Widen columns from VARCHAR to TEXT for complex nested JSON
ALTER TABLE insp_submission_details
  MODIFY COLUMN condition_logic TEXT NULL,
  MODIFY COLUMN scoring_config TEXT NULL,
  MODIFY COLUMN validation_rules TEXT NULL;

ALTER TABLE insp_template_items
  MODIFY COLUMN condition_logic TEXT NULL;

ALTER TABLE insp_template_sections
  MODIFY COLUMN condition_logic TEXT NULL;

-- 2. Batch upgrade V1 condition_logic → V2 in template items
UPDATE insp_template_items
SET condition_logic = JSON_OBJECT(
  'version', 2,
  'conditions', JSON_OBJECT(
    'logicOp', 'and',
    'rules', JSON_ARRAY(
      JSON_OBJECT(
        'field', JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.when')),
        'operator', JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.operator')),
        'value', IFNULL(JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.value')), '')
      )
    )
  ),
  'actions', JSON_ARRAY(
    JSON_OBJECT('type', JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.action')))
  )
)
WHERE condition_logic IS NOT NULL
  AND JSON_VALID(condition_logic)
  AND JSON_EXTRACT(condition_logic, '$.when') IS NOT NULL
  AND JSON_EXTRACT(condition_logic, '$.version') IS NULL;

-- 3. Batch upgrade V1 → V2 in template sections
UPDATE insp_template_sections
SET condition_logic = JSON_OBJECT(
  'version', 2,
  'conditions', JSON_OBJECT(
    'logicOp', 'and',
    'rules', JSON_ARRAY(
      JSON_OBJECT(
        'field', JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.when')),
        'operator', JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.operator')),
        'value', IFNULL(JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.value')), '')
      )
    )
  ),
  'actions', JSON_ARRAY(
    JSON_OBJECT('type', JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.action')))
  )
)
WHERE condition_logic IS NOT NULL
  AND JSON_VALID(condition_logic)
  AND JSON_EXTRACT(condition_logic, '$.when') IS NOT NULL
  AND JSON_EXTRACT(condition_logic, '$.version') IS NULL;

-- 4. Batch upgrade V1 → V2 in submission details
UPDATE insp_submission_details
SET condition_logic = JSON_OBJECT(
  'version', 2,
  'conditions', JSON_OBJECT(
    'logicOp', 'and',
    'rules', JSON_ARRAY(
      JSON_OBJECT(
        'field', JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.when')),
        'operator', JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.operator')),
        'value', IFNULL(JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.value')), '')
      )
    )
  ),
  'actions', JSON_ARRAY(
    JSON_OBJECT('type', JSON_UNQUOTE(JSON_EXTRACT(condition_logic, '$.action')))
  )
)
WHERE condition_logic IS NOT NULL
  AND JSON_VALID(condition_logic)
  AND JSON_EXTRACT(condition_logic, '$.when') IS NOT NULL
  AND JSON_EXTRACT(condition_logic, '$.version') IS NULL;
