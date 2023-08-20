package org.team100.lib.rrt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.team100.lib.graph.Graph;
import org.team100.lib.graph.Link;
import org.team100.lib.graph.Node;
import org.team100.lib.index.KDModel;
import org.team100.lib.index.KDNode;
import org.team100.lib.index.Util;
import org.team100.lib.planner.Solver;
import org.team100.lib.space.Path;

import edu.unc.robotics.prrts.RobotModel;
import edu.unc.robotics.prrts.Sample;
import edu.unc.robotics.prrts.kdtree.KDNearNode;
import edu.unc.robotics.prrts.tree.NearNode;

public class RRTStar<T extends KDModel & RobotModel> implements Solver {
    private final T _model;
    private final KDNode<Node> _rootNode;
    private final Sample _sample;
    private final double _gamma;
    private Link _bestPath;

    public RRTStar(T model, Sample sample, double gamma) {
        if (gamma < 1.0) {
            throw new IllegalArgumentException("invalid gamma, must be >= 1.0");
        }
        _model = model;
        _rootNode = new KDNode<Node>(new Node(model.initial()));
        _sample = sample;
        _gamma = gamma;
        _bestPath = null;
    }

    /**
     * @return true if a new sample was added.
     */
    @Override
    public boolean step(int stepNo) {
        double[] newConfig = _sample.get();

        // this is wrong, we want to check the steered sample.
        if (!_model.clear(newConfig)) {
            return false;
        }

        double radius = _gamma * Math.pow(
                Math.log(stepNo + 1) / (stepNo + 1),
                1.0 / _model.dimensions());

        List<NearNode> nearNodes = new ArrayList<>();
        Util.near(_model, _rootNode, newConfig, radius, (node, dist) -> {
            if (node.getIncoming() != null)
                nearNodes.add(new NearNode(node, dist));
        });

        if (nearNodes.isEmpty()) {

            KDNearNode<Node> nearResult = Util.nearest(_model, _rootNode, newConfig);
            Node nearest = nearResult._nearest;
            double distToNearest = nearResult._dist;

            if (distToNearest > radius) {
                // usually the "nearest" node is outside the radius, so bring it closer
                _model.steer(nearest.getState(), newConfig, radius / distToNearest);
            }

            if (!_model.clear(newConfig)) {
                return false;
            }

            if (!_model.link(nearest.getState(), newConfig)) {
                return false;
            }

            // This should be radius, but might be off slightly so we
            // recalculate just to be safe.
            distToNearest = _model.dist(newConfig, nearest.getState());

            // the new node has the new sampled config, the distance(cost) to the
            // nearest other node we found above, and the "parent" is the "link"
            // from that nearest node.
            Node newTarget = new Node(newConfig);
            Link newLink = Graph.newLink(nearest, newTarget, distToNearest);

            _bestPath = Graph.chooseBestPath(_model, _bestPath, newLink);

            Util.insert(_model, _rootNode, newTarget);
            return true;
        }

        // Sort the array from nearest to farthest. After sorting
        // we can traverse the array sequentially and select the first
        // configuration that can link. We know that anything after it
        // in the array will be further away, and thus potentially save
        // a lot of calls to the (usually) expensive link() method.
        Collections.sort(nearNodes);

        Iterator<NearNode> ni = nearNodes.iterator();
        while (ni.hasNext()) {
            NearNode nn = ni.next();
            ni.remove();

            if (!_model.link(nn.node.getState(), newConfig)) {
                continue;
            }

            // Found a linkable configuration. Create the node
            // and link it in here.

            Node newTarget = new Node(newConfig);
            Link newLink = Graph.newLink(nn.node, newTarget, nn.linkDist);

            _bestPath = Graph.chooseBestPath(_model, _bestPath, newLink);

            Util.insert(_model, _rootNode, newTarget);

            // For the remaining nodes in the near list, rewire
            // their links to go through the newly inserted node
            // if doing so is feasible and would shorten their path
            //
            // We go through the remaining list in reverse order to
            // reduce the number of rewirings we do on the farther nodes.
            // If we went from nearest to farthest, the far nodes might
            // rewire through the near nodes, then through the newly added
            // node.

            ListIterator<NearNode> li = nearNodes.listIterator(nearNodes.size());
            while (li.hasPrevious()) {
                NearNode jn = li.previous();

                // rewiring needs to be informed by the dynamics; turn it off for now

                if (Graph.rewire(_model, newTarget, jn.node, jn.linkDist)) {
                    _bestPath = Graph.chooseBestPath(_model, _bestPath, newTarget.getIncoming());
                }
            }

            return true;
        }

        // if we're here, we've looped through the entire near list and
        // found no nodes that can be linked through. We return false
        // indicating we failed to add a node.
        return false;
    }

    @Override
    public Iterable<Node> getNodes() {
        return Util.values(_rootNode);
    }

    @Override
    public Path getBestPath() {
        Link link = _bestPath;
        if (link == null) {
            return null;
        }
        Node node = link.get_target();

        // Collect the states along the path (backwards)
        List<double[]> configs = new LinkedList<double[]>();
        // Since we're visiting all the nodes it's very cheap to verify the total distance
        double totalDistance = 0;
        while (true) {
            configs.add(node.getState());
            Link incoming = node.getIncoming();
            if (incoming == null)
                break;
            totalDistance += incoming.get_linkDist();
            node = incoming.get_source();
        }
        // now we have the forwards list of states
        Collections.reverse(configs);

        return new Path(totalDistance, configs);
    }
}