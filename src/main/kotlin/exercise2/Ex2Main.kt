package exercise2

import de.hpi.dbs2.exercise2.AbstractBPlusTree
import de.hpi.dbs2.exercise2.BPlusTreeNode
import de.hpi.dbs2.exercise2.*

fun main() {
    val order = 4

    val root = BPlusTreeNode.buildTree(order,
        arrayOf(
            entryArrayOf(
                2 to ref(0),
                3 to ref(1),
                5 to ref(2)
            ), entryArrayOf(
                7 to ref(3),
                11 to ref(4)
            )
        ), arrayOf(
            entryArrayOf(
                13 to ref(5),
                17 to ref(6),
                19 to ref(7),
            ), entryArrayOf(
                23 to ref(8),
                29 to ref(9)
            ), entryArrayOf(
                31 to ref(10),
                37 to ref(11),
                41 to ref(12)
            ), entryArrayOf(
                43 to ref(13),
                47 to ref(14)
            )
        )
    )
    //println(root)

    val tree: AbstractBPlusTree = BPlusTreeKotlin(root)
    println(tree)

    val emptyTree = BPlusTreeKotlin(4)

    val leafNode = LeafNode(4)
    //println(leafNode)
    val innerNode = InnerNode(4)
    //println(innerNode)

    /*
     * playground
     * ~ feel free to experiment with the tree and tree nodes here
     */

    // Trigger Overwrite
    var double : ValueReference?
    double = tree.insert(7, ref(42))
    print(double)

    val keys = mutableListOf<Int>(6, 38, 25, 18, 9, 45, 39, 23, 2, 28, 44, 49, 24, 10, 26, 11, 37, 0, 48, 35)

    for (key in keys) {
        if(key == 35) {
        }
        emptyTree.insert(key, ref(1))
    }

    // Trigger insert with space left
    tree.insert(8, ref(20))

    // Trigger insert without space left (requires update of parent node)
    tree.insert(9, ref(20))

    tree.insert(4, ref(420000000))
    tree.insert(6, ref(420000000))
    tree.insert(10, ref(420000000))

    // trigger insert with parent split
    tree.insert(12, ref(420000000))

    tree.insert(42, ref(20734))
    tree.insert(48, ref(20734))
    tree.insert(49, ref(20734))
    tree.insert(50, ref(20734))

    // trigger insert with new root
    tree.insert(51, ref(20734))
    println(tree)

    val root2 = BPlusTreeNode.buildTree(order,
        entryArrayOf(
            2 to ref(0),
            7 to ref(3),
            11 to ref(4)
        )
    )

    val tree2: AbstractBPlusTree = BPlusTreeKotlin(root2)
    println(tree2)

}
