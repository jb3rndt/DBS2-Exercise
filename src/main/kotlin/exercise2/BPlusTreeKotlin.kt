package exercise2

import de.hpi.dbs2.ChosenImplementation
import de.hpi.dbs2.exercise2.*
import java.util.*

@ChosenImplementation(true)
class BPlusTreeKotlin : AbstractBPlusTree {
    constructor(order: Int) : super(order)
    constructor(rootNode: BPlusTreeNode<*>) : super(rootNode)

    override fun insert(key: Int, value: ValueReference): ValueReference? {
        // first time initial root node overflows
        if (this.rootNode is InitialRootNode && this.rootNode.isFull) {
            val root = this.rootNode as InitialRootNode
            val keys = mutableListOf<Int>(key, *root.keys)
            keys.sort()
            val index = keys.indexOf(key)
            val references = mutableListOf<ValueReference>(*root.references)
            references.add(index, value)
            val mid = (keys.size / 2)

            val entries = keys.mapIndexed{ i, k -> Entry(k, references[i])}

            val n1 = LeafNode(root.order, *entries.subList(0, mid).toTypedArray())
            val n2 = LeafNode(root.order, *entries.subList(mid, entries.size).toTypedArray())
            n1.nextSibling = n2
            n2.nextSibling = null
            this.rootNode = InnerNode(root.order, *mutableListOf<BPlusTreeNode<*>>(n1, n2).toTypedArray())
            return null
        }

        // find necessary leaf node and track path
        val treeStack = mutableListOf<BPlusTreeNode<*>>()
        treeStack.add(this.rootNode)
        var currentNode = this.rootNode
        while(currentNode is InnerNode) {
            currentNode = currentNode.selectChild(key)
            treeStack.add(currentNode)
        }

        // overwrite key if it already exists
        val leaf: LeafNode = treeStack.last() as LeafNode
        if (leaf.keys.contains(key)) {
            val oldValue = leaf.references[leaf.keys.indexOf(key)]
            leaf.references[leaf.keys.indexOf(key)] = value
            return oldValue
        }

        // Key does not exist
        if(!leaf.isFull) {
            // key can be inserted without split
            var insertIndex = leaf.keys.filterNotNull().indexOfFirst { it > key }
            insertIndex = if(insertIndex == -1) leaf.keys.indexOfFirst {it == null} else insertIndex
            val adjustedKeys = leaf.keys.toMutableList()
            adjustedKeys.add(insertIndex, key)
            val adjustedReferences = leaf.references.toMutableList()
            adjustedReferences.add(insertIndex, value)
            for (i in 0 until leaf.keys.size) {
                leaf.keys[i] = adjustedKeys[i]
                leaf.references[i] = adjustedReferences[i]
            }
        } else {
            // split leaf in two
            val keys = mutableListOf(key, *leaf.keys)
            keys.sort()
            val index = keys.indexOf(key)
            val references = mutableListOf<ValueReference>(*leaf.references)
            references.add(index, value)
            val mid = keys.size / 2

            val entries = keys.mapIndexed{ i, k -> Entry(k, references[i])}

            for(i in 0 until leaf.keys.size){
                if(i < mid) {
                    leaf.keys[i] = keys[i]
                    leaf.references[i] = references[i]
                } else {
                    leaf.keys[i] = null
                    leaf.references[i] = null
                }
            }
            var n2: BPlusTreeNode<*> = LeafNode(rootNode.order, *entries.subList(mid, entries.size).toTypedArray())
            (n2 as LeafNode).nextSibling = leaf.nextSibling
            leaf.nextSibling = n2

            treeStack.removeLast()

            // update parent nodes
            while(treeStack.isNotEmpty()) {
                val parentNode = treeStack.last() as InnerNode
                treeStack.removeLast()

                if (!parentNode.isFull) {
                    // key can be inserted without split
                    var indexx = parentNode.keys.filterNotNull().indexOfFirst { it > n2.smallestKey }
                    indexx = if(indexx == -1) parentNode.keys.indexOfFirst {it == null} else indexx
                    val adjustedKeys = parentNode.keys.toMutableList()
                    adjustedKeys.add(indexx, n2.smallestKey)
                    val adjustedReferences = parentNode.references.toMutableList()
                    adjustedReferences.add(indexx+1, n2)
                    for (i in 0 until parentNode.keys.size) {
                        parentNode.keys[i] = adjustedKeys[i]
                        parentNode.references[i+1] = adjustedReferences[i+1]
                    }
                    break
                } else {
                    val newKeys = mutableListOf<Int>(key, *parentNode.keys)
                    newKeys.sort()
                    val keyIndex = newKeys.indexOf(key)
                    val newReferences = mutableListOf<BPlusTreeNode<*>>(*parentNode.references)
                    newReferences.add(keyIndex + 1, n2)

                    val mid = newKeys.size / 2
                    val n1 = InnerNode(rootNode.order, *newReferences.subList(0, mid).toTypedArray())
                    n2 = InnerNode(rootNode.order, *newReferences.subList(mid, newReferences.size).toTypedArray())
                    for(i in 0 until parentNode.keys.size){
                        if(i < mid) {
                            parentNode.keys[i] = n1.keys[i]
                            parentNode.references[i] = newReferences[i]
                        } else {
                            parentNode.keys[i] = null
                            parentNode.references[i] = null
                            parentNode.references[i+1] = null
                        }
                    }

                    if (treeStack.isEmpty()) {
                        // create new root
                        val nodes = mutableListOf<BPlusTreeNode<*>>(n1, n2)
                        val newRoot = InnerNode(rootNode.order, *nodes.toTypedArray())
                        this.rootNode = newRoot
                        break
                    } else {
                        // N1 and N2 have been overwritten, so their splitkey
                        // can be inserted in the parent node just by continueing with this loop.
                        continue
                    }
                }
            }
        }


        // Find LeafNode in which the key has to be inserted.
        //   It is a good idea to track the "path" to the LeafNode in a Stack or something alike.
        // Does the key already exist? Overwrite!
        //   leafNode.references[pos] = value;
        //   But remember return the old value!
        // New key - Is there still space?
        //   leafNode.keys[pos] = key;
        //   leafNode.references[pos] = value;
        //   Don't forget to update the parent keys and so on...
        // Otherwise,
        //   Split the LeafNode in two!
        //   Is parent node root?
        //     update rootNode = ... // will have only one key
        //   Was node instanceof LeafNode?
        //     update parentNode.keys[?] = ...
        //   Don't forget to update the parent keys and so on...

        // Check out the exercise slides for a flow chart of this logic.
        // If you feel stuck, try to draw what you want to do and
        // check out Ex2Main for playing around with the tree by e.g. printing or debugging it.
        // Also check out all the methods on BPlusTreeNode and how they are implemented or
        // the tests in BPlusTreeNodeTests and BPlusTreeTests!
        return null
    }
}
