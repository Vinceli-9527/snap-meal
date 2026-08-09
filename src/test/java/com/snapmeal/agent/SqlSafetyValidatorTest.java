package com.snapmeal.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SqlSafetyValidator 纯离线单测：只读白名单与注入防护。 */
class SqlSafetyValidatorTest {

    private final SqlSafetyValidator validator = new SqlSafetyValidator();

    private void assertValid(String sql) {
        assertTrue(!validator.findError(sql).isPresent(), "应放行：" + sql);
    }

    private void assertRejected(String sql, String reasonFragment) {
        java.util.Optional<String> error = validator.findError(sql);
        assertTrue(error.isPresent(), "应拦截：" + sql);
        if (reasonFragment != null) {
            assertTrue(error.get().contains(reasonFragment), sql + " -> " + error.get());
        }
    }

    // ── 合法只读查询 ──────────────────────────────────────────────

    @Test
    void acceptsSimpleSelect() {
        assertValid("select * from orders");
        assertValid("SELECT * FROM orders");
        assertValid("select * from orders where status = 2");
    }

    @Test
    void acceptsAggregationWithGroupBy() {
        assertValid("select status, count(*) as cnt from orders group by status order by cnt desc");
        assertValid("select sum(amount) from orders where pay_status = 1");
    }

    @Test
    void acceptsWordBoundaryColumnNames() {
        // update/create 作为列名前缀不应误伤
        assertValid("select updated_at, create_time from orders");
        assertValid("select setmeal_id from order_detail");
    }

    @Test
    void acceptsCaseInsensitiveSelect() {
        assertValid("SeLeCt id, name FROM dish");
    }

    // ── 只读白名单：非 SELECT 一律拒绝 ─────────────────────────────

    @Test
    void rejectsEmptyOrNull() {
        assertRejected("", "为空");
        assertRejected("   ", "为空");
        assertRejected(null, "为空");
    }

    @Test
    void rejectsDmlAndDdlKeywords() {
        assertRejected("update orders set status = 1", "UPDATE");
        assertRejected("delete from orders", "DELETE");
        assertRejected("insert into orders(id) values (1)", "INSERT");
        assertRejected("drop table orders", "DROP");
        assertRejected("alter table orders add column x int", "ALTER");
        assertRejected("truncate table orders", "TRUNCATE");
        assertRejected("create table hack(x int)", "CREATE");
        assertRejected("begin; select 1", "单条");
    }

    @Test
    void rejectsMultiStatementWithSemicolon() {
        assertRejected("select 1; drop table orders", "分号");
        assertRejected("select 1; select 2", "分号");
    }

    @Test
    void rejectsCommentBypass() {
        assertRejected("select * from orders -- drop", "注释");
        assertRejected("select * from orders /* drop */", "注释");
        assertRejected("select * from orders # drop", "注释");
    }

    @Test
    void rejectsDangerousFunctionsAndFragments() {
        assertRejected("select load_file('/etc/passwd')", "load_file");
        assertRejected("select * from orders into outfile '/tmp/x'", "outfile");
        assertRejected("select * from information_schema.tables", "information_schema");
    }

    @Test
    void rejectsNonSelectEntry() {
        assertRejected("set @x = 1", "SELECT");
        assertRejected("  \n values (1)", "SELECT");
        assertEquals("仅允许执行 SELECT 只读查询", validator.findError("values (1)").get());
    }
}
