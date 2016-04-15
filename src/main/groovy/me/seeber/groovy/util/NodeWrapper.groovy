/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2015, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package me.seeber.groovy.util

import groovy.transform.TypeChecked

/**
 * Wrapper for {@link Node} that adds some convenience methods to manipulate node trees
 */
@TypeChecked
class NodeWrapper {

    /**
     * Parent node
     */
    private final NodeWrapper parent

    /**
     * Wrapped XML node
     */
    private final Node node

    /**
     * Child nodes
     */
    private List<NodeWrapper> children

    /**
     * Create a new root node
     *
     * @param node XML Node to wrap
     */
    NodeWrapper(Node node) {
        this(null, node)
    }

    /**
     * Create a new child node
     *
     * @param parent Parent node
     * @param node Wrapped child node
     */
    NodeWrapper(NodeWrapper parent, Node node) {
        if(node == null) {
            throw new NullPointerException("node")
        }

        this.parent = parent
        this.node = node
    }

    /**
     * Get first child node by name
     *
     * @param name Node name
     * @return First child node or <code>null</code> if there is no child node with this name
     */
    NodeWrapper getAt(String name) {
        NodeWrapper child = getChildren().find {
            it.node.name() == name
        }

        if(child == null) {
            child = new NodeWrapper(this, new Node((Node)null, name))
            children << child
        }

        child
    }

    /**
     * Insert the XML node into the tree
     *
     * @return This node
     */
    NodeWrapper insert() {
        if(parent != null && node.parent() == null) {
            parent.insert()
            parent.node.append(node)
        }

        this
    }

    /**
     * Get the name of the node
     *
     * @return Node name
     */
    String getName() {
        node.name()
    }

    /**
     * Get the text content of the node
     *
     * @return Node text
     */
    String getText() {
        node.text()
    }

    /**
     * Set the text content of the node, also inserts the node
     *
     * @param text Node text to set
     */
    void setText(String text) {
        insert()
        node.value = text
    }

    /**
     * Insert and initialize the node with the supplied text conent
     *
     * @param text Node text
     * @return This node
     */
    NodeWrapper init(String text) {
        if(text != null && text.trim().length() > 0) {
            insert()

            if(node.text().trim().length() == 0) {
                node.value = text
            }
        }

        this
    }

    /**
     * Get the child nodes
     *
     * @return Child nodes
     */
    List<NodeWrapper> getChildren() {
        if(children == null) {
            children = node.children().collect {
                new NodeWrapper(this, (Node)it)
            }
        }

        children
    }

    /**
     * Convert the node to a string
     */
    String toString() {
        "${node.name}[${children.collect{ NodeWrapper n -> n.name }.join(', ')}]"
    }
}
