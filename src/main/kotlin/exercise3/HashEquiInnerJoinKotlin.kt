package exercise3

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.dbms.Block
import de.hpi.dbs2.dbms.BlockManager
import de.hpi.dbs2.dbms.Operation
import de.hpi.dbs2.dbms.Relation
import de.hpi.dbs2.exercise3.InnerJoinOperation
import de.hpi.dbs2.exercise3.JoinAttributePair
import java.lang.Math.abs
import java.util.function.Consumer

@ChosenImplementation(true)
class HashEquiInnerJoinKotlin(
    blockManager: BlockManager,
    leftColumnIndex: Int,
    rightColumnIndex: Int,
) : InnerJoinOperation(
    blockManager,
    JoinAttributePair.EquiJoinAttributePair(
        leftColumnIndex,
        rightColumnIndex
    )
) {
    override fun estimatedIOCost(
        leftInputRelation: Relation,
        rightInputRelation: Relation
    ): Int {
        return 3*(leftInputRelation.estimatedBlockCount() + rightInputRelation.estimatedBlockCount())
    }

    override fun join(
        leftInputRelation: Relation,
        rightInputRelation: Relation,
        outputRelation: Relation
    ) {

        val bucketCount: Int = blockManager.freeBlocks - 1

        val swapped = rightInputRelation.estimatedBlockCount() > leftInputRelation.estimatedBlockCount()

        val outerRelation = if (swapped) rightInputRelation else leftInputRelation
        val innerRelation = if (swapped) leftInputRelation else rightInputRelation

        var outerBuckets = partition(outerRelation, if (swapped) joinAttributePair.rightColumnIndex else joinAttributePair.leftColumnIndex, bucketCount)
        var innerBuckets = partition(innerRelation, if(swapped) joinAttributePair.leftColumnIndex else joinAttributePair.rightColumnIndex, bucketCount)

        if(innerBuckets.maxOf { it.value.size } + 2 > blockManager.freeBlocks){
            throw Operation.RelationSizeExceedsCapacityException()
        }

        var outputBlock = blockManager.allocate(true)
        for(hash in outerBuckets.keys){
            var outerBlocks = outerBuckets[hash]!!
            var innerBlocks = innerBuckets[hash]
            if (innerBlocks != null) {
                var rs = innerBlocks.map { blockManager.load(it) }
                for(l in outerBlocks) {
                    val outerBlock = blockManager.load(l)
                    for(innerBlock in rs){
                        joinBlocks(if (swapped) innerBlock else outerBlock, if (swapped) outerBlock else innerBlock, buildOutputColumns(leftInputRelation, rightInputRelation), Consumer {
                            if (outputBlock.isFull()) {
                                outputRelation.getBlockOutput().move(outputBlock)
                                outputBlock = blockManager.allocate(true)
                            }
                            outputBlock.append(it)
                        })
                    }
                    blockManager.release(outerBlock, false)
                }
                rs.forEach { blockManager.release(it, false) }
            }
        }

        if (!outputBlock.isEmpty()) {
            outputRelation.getBlockOutput().move(outputBlock)
        } else {
            blockManager.release(outputBlock, false)
        }
    }

    private fun partition(relation: Relation, columnIndex: Int, bucketCount: Int) : MutableMap<Int, MutableList<Block>> {
        val buckets = mutableMapOf<Int, MutableList<Block>>()
        for(i in 0 until bucketCount){
            buckets[i] = mutableListOf(blockManager.allocate(true))
        }

        for (block in relation) {
            val b = blockManager.load(block)
            for (tuple in b) {
                val hash = kotlin.math.abs(tuple[columnIndex].hashCode()) % bucketCount
                if (buckets[hash] == null || buckets[hash]!!.isEmpty()) {
                    throw IllegalArgumentException()
                }
                if (buckets[hash]?.last()?.isFull() == true) {
                    buckets[hash]!![buckets[hash]!!.size - 1] = blockManager.release(buckets[hash]!!.last(), true)!!
                    buckets[hash]!!.add(blockManager.allocate(true))
                }
                buckets[hash]!!.last().append(tuple)
            }
            blockManager.release(b, false)
        }

        for(bucket in buckets){
            // write remaining buckets (blocks) to disc
            bucket.value[bucket.value.size-1] = blockManager.release(bucket.value.last(), true)!!
        }
        return buckets
    }
}