ALTER TABLE t_prodfamily
    ADD COLUMN associd bigint;

UPDATE t_prodfamily
SET associd = t_cmassocdef.associd
FROM t_prodfamily tp
LEFT JOIN t_cmassocdef ON t_cmassocdef.companyid = tp.companyid;

ALTER TABLE t_prodfamily
    DROP COLUMN companyid;
