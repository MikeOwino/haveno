/*
 * This file is part of Haveno.
 *
 * Haveno is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Haveno is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Haveno. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.network;

import bisq.network.p2p.NodeAddress;
import bisq.network.p2p.network.NetworkFilter;

import bisq.common.config.Config;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoreNetworkFilter implements NetworkFilter {
    private final Set<NodeAddress> bannedPeersFromOptions = new HashSet<>();
    private Function<NodeAddress, Boolean> bannedNodeFunction;

    /**
     * @param banList  List of banned peers from program argument
     */
    @Inject
    public CoreNetworkFilter(@Named(Config.BAN_LIST) List<String> banList) {
        banList.stream().map(NodeAddress::new).forEach(bannedPeersFromOptions::add);
    }

    @Override
    public void setBannedNodeFunction(Function<NodeAddress, Boolean> bannedNodeFunction) {
        this.bannedNodeFunction = bannedNodeFunction;
    }

    @Override
    public boolean isPeerBanned(NodeAddress nodeAddress) {
        return bannedPeersFromOptions.contains(nodeAddress) ||
                bannedNodeFunction != null && bannedNodeFunction.apply(nodeAddress);
    }
}
