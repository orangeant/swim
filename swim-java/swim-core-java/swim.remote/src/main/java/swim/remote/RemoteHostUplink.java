// Copyright 2015-2019 SWIM.AI inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package swim.remote;

import java.net.InetSocketAddress;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import swim.api.auth.Identity;
import swim.concurrent.PullContext;
import swim.concurrent.PullRequest;
import swim.runtime.LinkBinding;
import swim.runtime.LinkContext;
import swim.runtime.LinkKeys;
import swim.structure.Value;
import swim.uri.Uri;
import swim.warp.Envelope;

final class RemoteHostUplink implements LinkContext, PullRequest<Envelope> {
  final RemoteHost host;

  final LinkBinding link;

  final Uri remoteNodeUri;

  final Value linkKey;

  final ConcurrentLinkedQueue<Envelope> downQueue;

  PullContext<? super Envelope> pullContext;

  volatile int status;

  RemoteHostUplink(RemoteHost host, LinkBinding link, Uri remoteNodeUri, Value linkKey) {
    this.host = host;
    this.link = link;
    this.remoteNodeUri = remoteNodeUri;
    this.linkKey = linkKey.commit();
    this.downQueue = new ConcurrentLinkedQueue<Envelope>();
  }

  RemoteHostUplink(RemoteHost host, LinkBinding link, Uri remoteNodeUri) {
    this(host, link, remoteNodeUri, LinkKeys.generateLinkKey());
  }

  public LinkBinding linkBinding() {
    return this.link;
  }

  public Uri nodeUri() {
    return this.link.nodeUri();
  }

  public Uri laneUri() {
    return this.link.laneUri();
  }

  public Value linkKey() {
    return this.linkKey;
  }

  public float prio() {
    return this.link.prio();
  }

  @Override
  public boolean isConnectedUp() {
    return this.host.isConnected();
  }

  @Override
  public boolean isRemoteUp() {
    return this.host.isRemote();
  }

  @Override
  public boolean isSecureUp() {
    return this.host.isSecure();
  }

  @Override
  public String securityProtocolUp() {
    return this.host.securityProtocol();
  }

  @Override
  public String cipherSuiteUp() {
    return this.host.cipherSuite();
  }

  @Override
  public InetSocketAddress localAddressUp() {
    return this.host.localAddress();
  }

  @Override
  public Identity localIdentityUp() {
    return this.host.localIdentity();
  }

  @Override
  public Principal localPrincipalUp() {
    return this.host.localPrincipal();
  }

  @Override
  public Collection<Certificate> localCertificatesUp() {
    return this.host.localCertificates();
  }

  @Override
  public InetSocketAddress remoteAddressUp() {
    return this.host.remoteAddress();
  }

  @Override
  public Identity remoteIdentityUp() {
    return this.host.remoteIdentity();
  }

  @Override
  public Principal remotePrincipalUp() {
    return this.host.remotePrincipal();
  }

  @Override
  public Collection<Certificate> remoteCertificatesUp() {
    return this.host.remoteCertificates();
  }

  public void queueDown(Envelope envelope) {
    this.downQueue.add(envelope);
    int oldStatus;
    int newStatus;
    do {
      oldStatus = this.status;
      newStatus = oldStatus | FEEDING_DOWN;
    } while (oldStatus != newStatus && !STATUS.compareAndSet(this, oldStatus, newStatus));
    if (oldStatus != newStatus) {
      this.link.feedDown();
    }
  }

  @Override
  public void pullDown() {
    final Envelope envelope = this.downQueue.poll();
    int oldStatus;
    int newStatus;
    do {
      oldStatus = this.status;
      newStatus = oldStatus & ~FEEDING_DOWN;
    } while (oldStatus != newStatus && !STATUS.compareAndSet(this, oldStatus, newStatus));
    if (envelope != null) {
      this.link.pushDown(envelope);
    }
    feedDownQueue();
  }

  void feedDownQueue() {
    int oldStatus;
    int newStatus;
    do {
      oldStatus = this.status;
      if (!this.downQueue.isEmpty()) {
        newStatus = oldStatus | FEEDING_DOWN;
      } else {
        newStatus = oldStatus;
      }
    } while (oldStatus != newStatus && !STATUS.compareAndSet(this, oldStatus, newStatus));
    if (oldStatus != newStatus) {
      this.link.feedDown();
    }
  }

  @Override
  public void feedUp() {
    int oldStatus;
    int newStatus;
    do {
      oldStatus = this.status;
      if ((oldStatus & PULLING_UP) == 0) {
        newStatus = oldStatus & ~FEEDING_UP | PULLING_UP;
      } else {
        newStatus = oldStatus | FEEDING_UP;
      }
    } while (oldStatus != newStatus && !STATUS.compareAndSet(this, oldStatus, newStatus));
    if ((oldStatus & PULLING_UP) == 0) {
      this.host.warpSocketContext.feed(this);
    }
  }

  @Override
  public void pull(PullContext<? super Envelope> pullContext) {
    this.pullContext = pullContext;
    this.link.pullUp();
  }

  @Override
  public void pushUp(Envelope envelope) {
    int oldStatus;
    int newStatus;
    do {
      oldStatus = this.status;
      newStatus = oldStatus & ~PULLING_UP;
    } while (oldStatus != newStatus && !STATUS.compareAndSet(this, oldStatus, newStatus));
    if (oldStatus != newStatus && this.pullContext != null) {
      final Envelope remoteEnvelope = envelope.nodeUri(this.remoteNodeUri);
      this.pullContext.push(remoteEnvelope);
      this.pullContext = null;
    }
  }

  @Override
  public void skipUp() {
    int oldStatus;
    int newStatus;
    do {
      oldStatus = this.status;
      newStatus = oldStatus & ~PULLING_UP;
    } while (oldStatus != newStatus && !STATUS.compareAndSet(this, oldStatus, newStatus));
    if (oldStatus != newStatus && this.pullContext != null) {
      this.pullContext.skip();
      this.pullContext = null;
    }
  }

  @Override
  public void closeUp() {
    this.host.closeUplink(this);
  }

  @Override
  public void didOpenDown() {
    // nop
  }

  public void didConnect() {
    this.link.didConnect();
  }

  public void didDisconnect() {
    this.link.didDisconnect();
    STATUS.set(this, 0);
  }

  @Override
  public void didCloseDown() {
    // nop
  }

  public void didCloseUp() {
    this.link.didCloseUp();
  }

  @Override
  public void traceUp(Object message) {
    this.host.trace(message);
  }

  @Override
  public void debugUp(Object message) {
    this.host.debug(message);
  }

  @Override
  public void infoUp(Object message) {
    this.host.info(message);
  }

  @Override
  public void warnUp(Object message) {
    this.host.warn(message);
  }

  @Override
  public void errorUp(Object message) {
    this.host.error(message);
  }

  static final int FEEDING_DOWN = 1 << 0;
  static final int FEEDING_UP = 1 << 1;
  static final int PULLING_UP = 1 << 2;

  static final AtomicIntegerFieldUpdater<RemoteHostUplink> STATUS =
      AtomicIntegerFieldUpdater.newUpdater(RemoteHostUplink.class, "status");
}
