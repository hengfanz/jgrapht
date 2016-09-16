/*
 * (C) Copyright 2015-2016, by Joris Kinable and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht.graph.specifics;

import java.io.*;
import java.util.*;

import org.jgrapht.graph.*;
import org.jgrapht.util.*;

/**
 * Fast implementation of DirectedSpecifics. This class uses additional data structures to improve
 * the performance of methods which depend on edge retrievals, e.g. getEdge(V u, V v),
 * containsEdge(V u, V v),addEdge(V u, V v). A disadvantage is an increase in memory consumption. If
 * memory utilization is an issue, use a {@link DirectedSpecifics} instead.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 *
 * @author Joris Kinable
 */
public class FastLookupDirectedSpecifics<V, E>
    extends DirectedSpecifics<V, E>
    implements Serializable
{
    private static final long serialVersionUID = 4089085208843722263L;

    /*
     * Maps a pair of vertices <u,v> to a set of edges {(u,v)}. In case of a multigraph, all edges
     * which touch both u,v are included in the set
     */
    protected Map<VertexPair<V>, ArrayUnenforcedSet<E>> touchingVerticesToEdgeMap;

    /**
     * Construct a new fast lookup directed specifics.
     * 
     * @param abstractBaseGraph the graph for which these specifics are for
     */
    public FastLookupDirectedSpecifics(AbstractBaseGraph<V, E> abstractBaseGraph)
    {
        this(abstractBaseGraph, new LinkedHashMap<>());
    }

    /**
     * Construct a new fast lookup directed specifics.
     * 
     * @param abstractBaseGraph the graph for which these specifics are for
     * @param vertexMap a container to use the edges
     */
    public FastLookupDirectedSpecifics(
        AbstractBaseGraph<V, E> abstractBaseGraph, Map<V, DirectedEdgeContainer<V, E>> vertexMap)
    {
        super(abstractBaseGraph, vertexMap);
        this.touchingVerticesToEdgeMap = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<E> getAllEdges(V sourceVertex, V targetVertex)
    {
        if (abstractBaseGraph.containsVertex(sourceVertex)
            && abstractBaseGraph.containsVertex(targetVertex))
        {
            Set<E> edges =
                touchingVerticesToEdgeMap.get(new VertexPair<>(sourceVertex, targetVertex));
            return edges == null ? Collections.emptySet() : new ArrayUnenforcedSet<>(edges);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E getEdge(V sourceVertex, V targetVertex)
    {
        List<E> edges = touchingVerticesToEdgeMap.get(new VertexPair<>(sourceVertex, targetVertex));
        if (edges == null || edges.isEmpty())
            return null;
        else
            return edges.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEdgeToTouchingVertices(E e)
    {
        V source = abstractBaseGraph.getEdgeSource(e);
        V target = abstractBaseGraph.getEdgeTarget(e);

        getEdgeContainer(source).addOutgoingEdge(e);
        getEdgeContainer(target).addIncomingEdge(e);

        VertexPair<V> vertexPair = new VertexPair<>(source, target);
        if (!touchingVerticesToEdgeMap.containsKey(vertexPair)) {
            ArrayUnenforcedSet<E> edgeSet = new ArrayUnenforcedSet<>();
            edgeSet.add(e);
            touchingVerticesToEdgeMap.put(vertexPair, edgeSet);
        } else
            touchingVerticesToEdgeMap.get(vertexPair).add(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeEdgeFromTouchingVertices(E e)
    {
        V source = abstractBaseGraph.getEdgeSource(e);
        V target = abstractBaseGraph.getEdgeTarget(e);

        getEdgeContainer(source).removeOutgoingEdge(e);
        getEdgeContainer(target).removeIncomingEdge(e);

        // Remove the edge from the touchingVerticesToEdgeMap. If there are no more remaining edges
        // for a pair
        // of touching vertices, remove the pair from the map.
        VertexPair<V> vertexPair = new VertexPair<>(source, target);
        if (touchingVerticesToEdgeMap.containsKey(vertexPair)) {
            ArrayUnenforcedSet<E> edgeSet = touchingVerticesToEdgeMap.get(vertexPair);
            edgeSet.remove(e);
            if (edgeSet.isEmpty())
                touchingVerticesToEdgeMap.remove(vertexPair);
        }
    }

}
