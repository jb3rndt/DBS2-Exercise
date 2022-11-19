package exercise1

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.dbms.Block
import de.hpi.dbs2.dbms.BlockManager
import de.hpi.dbs2.dbms.BlockOutput
import de.hpi.dbs2.dbms.Relation
import de.hpi.dbs2.dbms.Tuple
import de.hpi.dbs2.dbms.utils.BlockSorter
import de.hpi.dbs2.exercise1.SortOperation
import kotlin.math.ceil

@ChosenImplementation(true)
class TPMMSKotlin(manager: BlockManager, sortColumnIndex: Int) :
        SortOperation(manager, sortColumnIndex) {
    override fun estimatedIOCost(relation: Relation): Int {
        // If the relation consists of n Blocks, Phase 1 needs 2 I/O-Operations per Block, thus 2*n.
        // Phase 2 has to read all blocks once, thus 1*n. We assume that the BlockOutput.output-method does not need
        // I/O-Operations, since there are none specified in the method's documentation. So the I/O Cost
        // estimates to 3*n under these assumptions.
        return 3 * relation.estimatedSize
    }

    override fun sort(relation: Relation, output: BlockOutput) {
        val listSize: Int = blockManager.freeBlocks
        val nLists: Int = ceil(relation.estimatedSize / listSize.toDouble()).toInt()
        if(nLists > blockManager.freeBlocks - 1)
            throw RelationSizeExceedsCapacityException()

        // Phase 1
        val lists = mutableListOf<MutableList<Block>>()
        val blockPointer = relation.iterator()
        for (i in 0 until nLists) {
            val blocks = mutableListOf<Block>()
            while (blockManager.freeBlocks > 0 &&
                    blockPointer.hasNext() &&
                    blockManager.usedBlocks < listSize) blocks.add(
                    blockManager.load(blockPointer.next())
            )

            BlockSorter.sort(
                    relation,
                    blocks,
                    relation.columns.getColumnComparator(sortColumnIndex)
            )

            val references = mutableListOf<Block>()
            for (block in blocks) {
                references.add(blockManager.release(block, true)!!)
            }
            lists.add(references)
        }

        // Phase 2:
        val outputBlock: Block = blockManager.allocate(true)
        val blocks = mutableListOf<Block>()
        lists.forEach { blocks.add(blockManager.load(it.first())) }
        val iterators = mutableListOf<Iterator<Tuple>>()
        blocks.forEach { iterators.add(it.iterator()) }
        val firstTuples = mutableListOf<Tuple>()
        iterators.forEach { firstTuples.add(it.next()) }

        while (lists.size > 0) {
            var min: Pair<Int, Tuple> = 0 to firstTuples.first()
            for (f in firstTuples.withIndex()) {
                if (relation.columns
                                .getColumnComparator(sortColumnIndex)
                                .compare(min.second, f.value) > 0
                )
                        min = f.index to f.value
            }

            if (outputBlock.isFull()) {
                output.output(outputBlock)
            }
            outputBlock.append(firstTuples[min.first])
            if (!iterators[min.first].hasNext()) {
                blocks[min.first].clear()
                blocks[min.first].close()
                lists[min.first].removeAt(0)
                if (lists[min.first].isNotEmpty()) {
                    blocks[min.first] = blockManager.load(lists[min.first].first())
                    iterators[min.first] = blocks[min.first].iterator()
                    firstTuples[min.first] = iterators[min.first].next()
                } else {
                    lists.removeAt(min.first)
                    blocks.removeAt(min.first)
                    iterators.removeAt(min.first)
                    firstTuples.removeAt(min.first)
                }
            } else {
                firstTuples[min.first] = iterators[min.first].next()
            }
        }
        if(!outputBlock.isEmpty()) {
            output.output(outputBlock)
        }
        blockManager.release(outputBlock, false)
    }
}
