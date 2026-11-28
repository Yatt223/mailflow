-- Le journal d'audit est en ajout seul : toute modification ou suppression est refusée.
CREATE OR REPLACE FUNCTION interdire_modification_audit() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'journal_audit est en ajout seul (% interdit)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_pas_de_modification
    BEFORE UPDATE OR DELETE ON journal_audit
    FOR EACH ROW EXECUTE FUNCTION interdire_modification_audit();

CREATE TRIGGER trg_audit_pas_de_truncate
    BEFORE TRUNCATE ON journal_audit
    FOR EACH STATEMENT EXECUTE FUNCTION interdire_modification_audit();