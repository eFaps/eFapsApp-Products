ALTER TABLE t_prodline
    ADD COLUMN associd bigint;

UPDATE t_prodline
SET associd = t_cmassocdef.associd
FROM t_prodline tp
LEFT JOIN t_cmassocdef ON t_cmassocdef.companyid = tp.companyid;

ALTER TABLE t_prodline
    DROP COLUMN companyid;
