/*
 * This file is part of Bisq.
 *
 * bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.monitor.metric;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

import bisq.common.app.Version;
import bisq.common.proto.network.NetworkEnvelope;
import bisq.core.proto.network.CoreNetworkProtoResolver;
import bisq.monitor.Metric;
import bisq.monitor.Reporter;
import bisq.network.p2p.NodeAddress;
import bisq.network.p2p.network.Connection;
import bisq.network.p2p.network.MessageListener;
import bisq.network.p2p.network.NetworkNode;
import bisq.network.p2p.network.NewTor;
import bisq.network.p2p.network.SetupListener;
import bisq.network.p2p.network.TorNetworkNode;
import bisq.network.p2p.peers.getdata.messages.GetDataResponse;
import bisq.network.p2p.peers.getdata.messages.PreliminaryGetDataRequest;
import bisq.network.p2p.storage.P2PDataStorage;
import bisq.network.p2p.storage.payload.PersistableNetworkPayload;
import bisq.network.p2p.storage.payload.ProtectedStorageEntry;
import bisq.network.p2p.storage.payload.ProtectedStoragePayload;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class P2PNetworkLoad extends Metric implements MessageListener, SetupListener {

    private static final String HOSTS = "run.hosts";
    private static final String TOR_PROXY_PORT = "run.torProxyPort";
    private NetworkNode networkNode;
    private final File torWorkingDirectory = new File("metric_p2pNetworkLoad");
    private int nonce;
    private Boolean ready = false;
    private Set<byte[]> hashes = new HashSet<>();

    public P2PNetworkLoad(Reporter reporter) {
        super(reporter);

        Version.setBaseCryptoNetworkId(0); // set to BTC_MAINNET
    }

    @Override
    protected void enable() {
        Thread sepp = new Thread(new Runnable() {
            
            @Override
            public void run() {
                synchronized (ready) {
                    while (!ready)
                        try {
                            ready.wait();
                        } catch (InterruptedException ignore) {
                        }
                    P2PNetworkLoad.super.enable();
                }
            }
        });
        sepp.start();
    }

    @Override
    public void configure(Properties properties) {
        super.configure(properties);
        networkNode = new TorNetworkNode(Integer.parseInt(configuration.getProperty(TOR_PROXY_PORT, "9053")),
                new CoreNetworkProtoResolver(), false,
                new NewTor(torWorkingDirectory, "", "", null));
        networkNode.start(this);
    }

    /**
     * synchronization helper.
     */
    private void await() {
        synchronized (torWorkingDirectory) {
            try {
                torWorkingDirectory.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void proceed() {
        synchronized (torWorkingDirectory) {
            torWorkingDirectory.notify();
        }
    }

    @Override
    protected void execute() {

        // for each configured host
        for (String current : configuration.getProperty(HOSTS, "").split(",")) {
            try {
                // parse Url
                URL tmp = new URL(current);
                NodeAddress target = new NodeAddress(tmp.getHost(), tmp.getPort());

                nonce = new Random().nextInt();
                SettableFuture<Connection> future = networkNode.sendMessage(target,
                        new PreliminaryGetDataRequest(nonce, hashes));

                Futures.addCallback(future, new FutureCallback<Connection>() {
                    @Override
                    public void onSuccess(Connection connection) {
                        connection.addMessageListener(P2PNetworkLoad.this);
                        log.debug("Send PreliminaryDataRequest to " + connection + " succeeded.");
                    }

                    @Override
                    public void onFailure(@NotNull Throwable throwable) {
                        log.error(
                                "Sending PreliminaryDataRequest failed. That is expected if the peer is offline.\n\tException="
                                        + throwable.getMessage());
                    }
                });

                await();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // report
    }

    @Override
    public void onMessage(NetworkEnvelope networkEnvelope, Connection connection) {
        if (networkEnvelope instanceof GetDataResponse) {

            GetDataResponse getDataResponse = (GetDataResponse) networkEnvelope;
            final Set<ProtectedStorageEntry> dataSet = getDataResponse.getDataSet();
            dataSet.stream().forEach(e -> {
                final ProtectedStoragePayload protectedStoragePayload = e.getProtectedStoragePayload();
                if (protectedStoragePayload == null) {
                    log.warn("StoragePayload was null: {}", networkEnvelope.toString());
                    return;
                }

                // memorize message hashes
                // TODO cleanup hash list once in a while
                hashes.add(P2PDataStorage.get32ByteHash(protectedStoragePayload));

                // For logging different data types
            });

            Set<PersistableNetworkPayload> persistableNetworkPayloadSet = getDataResponse
                    .getPersistableNetworkPayloadSet();
            if (persistableNetworkPayloadSet != null) {
                persistableNetworkPayloadSet.stream().forEach(persistableNetworkPayload -> {

                    // memorize message hashes
                    // TODO cleanup hash list once in a while
                    hashes.add(persistableNetworkPayload.getHash());

                    // For logging different data types
                });
            }

            connection.removeMessageListener(this);
            proceed();
        }
    }

    @Override
    public void onTorNodeReady() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onHiddenServicePublished() {
        synchronized (ready) {
            ready.notify();
            ready = true;
        }
    }

    @Override
    public void onSetupFailed(Throwable throwable) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRequestCustomBridges() {
        // TODO Auto-generated method stub

    }
}
