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
    override fun estimatedIOCost(relation: Relation): Int = TODO()

    override fun sort(relation: Relation, output: BlockOutput) {
        val size: Int = relation.estimatedSize
        val nLists: Int =
                ceil(relation.estimatedSize / (blockManager.freeBlocks - 1).toDouble()).toInt()
        val listSize: Int = ceil(relation.estimatedSize / nLists.toDouble()).toInt()

        // Phase 1
        // Lade jeweils so viele Tupel, wie in Hauptspeicher passen
        // □ Sortiere Teilstücke (im Hauptspeicher)
        // □ Schreibe sortierte Teilstücke auf Festplatte zurück
        // □ Ergebnis: viele sortierte Teillisten (auf Festplatte)
        val lists = mutableListOf<MutableList<Block>>()
        var blockPointer = relation.iterator()
        for (i in 1..nLists-1) {
            var blocks = mutableListOf<Block>()
            while (blockManager.freeBlocks > 0 &&
                    blockPointer.hasNext() &&
                    blockManager.usedBlocks <= listSize) blocks.add(
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
                // hier passiert I/O;
            }
            lists.add(references)
        }

        // Phase 2:
        // □ Merge alle sortierten Teillisten in einzige große Liste

        val outputBlock: Block = blockManager.allocate(true)
        val blocks = mutableListOf<Block>()
        lists.forEach { blocks.add(blockManager.load(it.first())) }
        val iterators = mutableListOf<Iterator<Tuple>>()
        blocks.forEach { iterators.add(it.iterator()) }

        while (lists.size > 0) {
            val firstTuples = blocks.map { it.first() }
            // for(i in 0..nLists-1) {
            //   firstTuples.add(blocks.get(i).get(0));
            // }
            // val firstTuples = iterators.withIndex().minBy{ (_, f) -> f.get(sortColumnIndex)!!
            // }?.index
            // val ffirstTuples = iterators.withIndex().minBy{ (_, f) -> f }?.index
            // flights.withIndex().minBy { (_, f) -> f.duration }?.index

            // firstTuples.sortedWith(relation.columns.getColumnComparator(sortColumnIndex));
            var min: Pair<Int, Tuple> = 0 to firstTuples.first()
            for (f in firstTuples.withIndex()) {
                // if(relation.columns.getColumnType(sortColumnIndex).toComparable(min.second[sortColumnIndex])
                //  >
                // relation.columns.getColumnType(sortColumnIndex).toComparable(f.value[sortColumnIndex])
                //  )
                if (relation.columns
                                .getColumnComparator(sortColumnIndex)
                                .compare(min.second, f.value) == 0
                )
                        min = f.index to f.value
            }

            if (outputBlock.isFull()) {
                output.output(outputBlock)
            }
            outputBlock.append(iterators.get(min.first).next())
            if (!iterators.get(min.first).hasNext()) {
                blocks.get(min.first).clear()
                blocks.get(min.first).close()
                lists.get(min.first).removeAt(0)
                if (lists.get(min.first).size > 1) {
                    blocks.set(min.first, blockManager.load(lists[min.first][0]))
                    iterators.set(min.first, blocks.get(min.first).iterator())
                } else {
                    lists.removeAt(min.first)
                    blocks.removeAt(min.first)
                    iterators.removeAt(min.first)
                }
            }
        }
        blockManager.release(outputBlock, false)
        println(blockManager.usedBlocks)
    }
}
