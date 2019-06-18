ALTER TABLE t_prodproduct
    ADD COLUMN associd bigint;

UPDATE t_prodproduct
SET associd = t_cmassocdef.associd
FROM t_prodproduct tp
LEFT JOIN t_cmassocdef ON t_cmassocdef.companyid = tp.companyid;

ALTER TABLE t_prodproduct
    DROP COLUMN companyid;
