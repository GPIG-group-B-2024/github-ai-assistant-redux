package uk.ac.york.gpig.teamb.aiassistant.testutils.databuilders

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Table
import org.jooq.TableField

@DslMarker annotation class TestDSL

abstract class TestDataBuilder<TRecord : TestDataBuilder<TRecord>> {
    abstract fun create(ctx: DSLContext): TRecord
}

abstract class TestDataWithIdBuilder<TRecord : TestDataWithIdBuilder<TRecord, TId>, TId> :
    TestDataBuilder<TRecord>() {
    abstract val id: TId
}

fun <T : Record, TBuilder : TestDataBuilder<TBuilder>> TBuilder.create(
    ctx: DSLContext,
    table: Table<T>,
    existenceCheck: () -> Condition,
    insertAction: () -> Int,
): TBuilder {
    val record = ctx.selectFrom(table).where(existenceCheck()).fetchOne()
    if (record != null) {
        return this
    }
    val insertResult = insertAction()
    if (insertResult != 1) {
        throw Exception("Error building test data")
    }
    Thread.sleep(0, 10000)
    return this
}

fun <T : Record, TId, TBuilder : TestDataWithIdBuilder<TBuilder, TId>> TBuilder.create(
    ctx: DSLContext,
    table: Table<T>,
    idField: TableField<T, TId>,
    insertAction: () -> Int,
): TBuilder = this.create(ctx, table, { idField.eq(this.id) }, insertAction)
