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

package swim.runtime.lane;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import swim.api.agent.AgentContext;
import swim.api.auth.Identity;
import swim.api.data.DataFactory;
import swim.api.downlink.Downlink;
import swim.api.lane.Lane;
import swim.api.policy.Policy;
import swim.api.uplink.Uplink;
import swim.codec.Decoder;
import swim.collections.FingerTrieSeq;
import swim.concurrent.Schedule;
import swim.concurrent.Stage;
import swim.http.HttpBody;
import swim.http.HttpRequest;
import swim.http.HttpResponse;
import swim.http.HttpStatus;
import swim.math.Z2Form;
import swim.runtime.AbstractTierBinding;
import swim.runtime.HttpBinding;
import swim.runtime.LaneBinding;
import swim.runtime.LaneContext;
import swim.runtime.LinkBinding;
import swim.runtime.LinkContext;
import swim.runtime.PushRequest;
import swim.runtime.TierContext;
import swim.runtime.uplink.UplinkModem;
import swim.store.DataBinding;
import swim.store.ListDataBinding;
import swim.store.MapDataBinding;
import swim.store.SpatialDataBinding;
import swim.store.ValueDataBinding;
import swim.structure.Value;
import swim.uri.Uri;
import swim.warp.CommandMessage;
import swim.warp.Envelope;

public abstract class LaneModel<View extends LaneView, U extends UplinkModem> extends AbstractTierBinding implements LaneBinding {
  protected LaneContext laneContext;

  volatile Object views; // View | LaneView[]

  volatile FingerTrieSeq<U> uplinks;

  public LaneModel() {
    this.uplinks = FingerTrieSeq.empty();
  }

  @Override
  public TierContext tierContext() {
    return this.laneContext;
  }

  @Override
  public LaneContext laneContext() {
    return this.laneContext;
  }

  @Override
  public void setLaneContext(LaneContext laneContext) {
    this.laneContext = laneContext;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T unwrapLane(Class<T> laneClass) {
    if (laneClass.isAssignableFrom(getClass())) {
      return (T) this;
    } else {
      final Object views = this.views;
      if (views instanceof LaneView) {
        return ((LaneView) views).unwrapLane(laneClass);
      } else if (views instanceof LaneView[]) {
        final LaneView[] viewArray = (LaneView[]) views;
        for (int i = 0, n = viewArray.length; i < n; i += 1) {
          final T lane = viewArray[i].unwrapLane(laneClass);
          if (lane != null) {
            return lane;
          }
        }
      }
      return null;
    }
  }

  protected abstract U createUplink(LinkBinding link);

  @Override
  public final Uri meshUri() {
    return this.laneContext.meshUri();
  }

  @Override
  public final Value partKey() {
    return this.laneContext.partKey();
  }

  @Override
  public final Uri hostUri() {
    return this.laneContext.hostUri();
  }

  @Override
  public final Uri nodeUri() {
    return this.laneContext.nodeUri();
  }

  @Override
  public final Uri laneUri() {
    return this.laneContext.laneUri();
  }

  @Override
  public final Value agentKey() {
    return this.laneContext.agentKey();
  }

  @Override
  public Policy policy() {
    return this.laneContext.policy();
  }

  @Override
  public Schedule schedule() {
    return this.laneContext.schedule();
  }

  @Override
  public Stage stage() {
    return this.laneContext.stage();
  }

  @Override
  public DataFactory data() {
    return this.laneContext.data();
  }

  @Override
  public Lane getLaneView(AgentContext agentContext) {
    final Object views = this.views;
    LaneView view;
    if (views instanceof LaneView) {
      view = (LaneView) views;
      if (agentContext == view.agentContext()) {
        return view;
      }
    } else if (views instanceof LaneView[]) {
      final LaneView[] viewArray = (LaneView[]) views;
      for (int i = 0, n = viewArray.length; i < n; i += 1) {
        view = viewArray[i];
        if (agentContext == view.agentContext()) {
          return view;
        }
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void openLaneView(Lane view) {
    Object oldLaneViews;
    Object newLaneViews;
    do {
      oldLaneViews = this.views;
      if (oldLaneViews instanceof LaneView) {
        newLaneViews = new LaneView[]{(LaneView) oldLaneViews, (LaneView) view};
      } else if (oldLaneViews instanceof LaneView[]) {
        final LaneView[] oldLaneViewArray = (LaneView[]) oldLaneViews;
        final int n = oldLaneViewArray.length;
        final LaneView[] newLaneViewArray = new LaneView[n + 1];
        System.arraycopy(oldLaneViewArray, 0, newLaneViewArray, 0, n);
        newLaneViewArray[n] = (LaneView) view;
        newLaneViews = newLaneViewArray;
      } else {
        newLaneViews = (LaneView) view;
      }
    } while (!VIEWS.compareAndSet(this, oldLaneViews, newLaneViews));
    didOpenLaneView((View) view);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void closeLaneView(Lane view) {
    Object oldLaneViews;
    Object newLaneViews;
    do {
      oldLaneViews = this.views;
      if (oldLaneViews instanceof LaneView) {
        if (oldLaneViews == view) {
          newLaneViews = null;
          continue;
        }
      } else if (oldLaneViews instanceof LaneView[]) {
        final LaneView[] oldLaneViewArray = (LaneView[]) oldLaneViews;
        final int n = oldLaneViewArray.length;
        if (n == 2) {
          if (oldLaneViewArray[0] == view) {
            newLaneViews = oldLaneViewArray[1];
            continue;
          } else if (oldLaneViewArray[1] == view) {
            newLaneViews = oldLaneViewArray[0];
            continue;
          }
        } else { // n > 2
          final LaneView[] newLaneViewArray = new LaneView[n - 1];
          int i = 0;
          while (i < n) {
            if (oldLaneViewArray[i] != view) {
              if (i < n - 1) {
                newLaneViewArray[i] = oldLaneViewArray[i];
              }
              i += 1;
            } else {
              break;
            }
          }
          if (i < n) {
            System.arraycopy(oldLaneViewArray, i + 1, newLaneViewArray, i, n - (i + 1));
            newLaneViews = newLaneViewArray;
            continue;
          }
        }
      }
      newLaneViews = oldLaneViews;
      break;
    } while (!VIEWS.compareAndSet(this, oldLaneViews, newLaneViews));
    if (oldLaneViews != newLaneViews) {
      didCloseLaneView((View) view);
    }
    if (newLaneViews == null) {
      close();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public FingerTrieSeq<LinkContext> getUplinks() {
    return (FingerTrieSeq<LinkContext>) (FingerTrieSeq<?>) this.uplinks;
  }

  @SuppressWarnings("unchecked")
  @Override
  public LinkBinding getUplink(Value linkKey) {
    final FingerTrieSeq<U> uplinks = (FingerTrieSeq<U>) (FingerTrieSeq<?>) this.uplinks;
    for (int i = 0, n = uplinks.size(); i < n; i += 1) {
      final U uplink = uplinks.get(i);
      if (linkKey.equals(uplink.linkKey())) {
        return uplink.linkBinding();
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void openUplink(LinkBinding link) {
    FingerTrieSeq<U> oldUplinks;
    FingerTrieSeq<U> newUplinks;
    final U uplink = createUplink(link);
    link.setLinkContext(uplink);
    do {
      oldUplinks = (FingerTrieSeq<U>) (FingerTrieSeq<?>) this.uplinks;
      newUplinks = oldUplinks.appended(uplink);
    } while (!UPLINKS.compareAndSet(this, oldUplinks, newUplinks));
    didUplink(uplink);
    // TODO: onEnter
  }

  @SuppressWarnings("unchecked")
  @Override
  public void closeUplink(Value linkKey) {
    FingerTrieSeq<U> oldUplinks;
    FingerTrieSeq<U> newUplinks;
    U linkContext;
    do {
      oldUplinks = (FingerTrieSeq<U>) (FingerTrieSeq<?>) this.uplinks;
      newUplinks = oldUplinks;
      linkContext = null;
      for (int i = 0, n = oldUplinks.size(); i < n; i += 1) {
        final U uplink = oldUplinks.get(i);
        if (linkKey.equals(uplink.linkKey())) {
          linkContext = uplink;
          newUplinks = oldUplinks.removed(i);
          break;
        }
      }
    } while (!UPLINKS.compareAndSet(this, oldUplinks, newUplinks));
    if (linkContext != null) {
      linkContext.linkBinding().didCloseUp();
      // TODO: onLeave
    }
  }

  @Override
  public void httpUplink(HttpBinding http) {
    final HttpLaneUplink httpContext = new HttpLaneUplink(this, http);
    http.setHttpContext(httpContext);
  }

  @SuppressWarnings("unchecked")
  protected void cueDown() {
    FingerTrieSeq<U> uplinks;
    do {
      uplinks = (FingerTrieSeq<U>) (FingerTrieSeq<?>) this.uplinks;
      for (int i = 0, n = uplinks.size(); i < n; i += 1) {
        final U u = uplinks.get(i);
        if (u.isConnected()) {
          u.cueDown();
        } else {
          closeUplink(u.linkKey());
        }
      }
    } while (uplinks != this.uplinks);
  }

  @SuppressWarnings("unchecked")
  protected void sendDown(Value body) {
    FingerTrieSeq<U> uplinks;
    do {
      uplinks = (FingerTrieSeq<U>) (FingerTrieSeq<?>) this.uplinks;
      for (int i = 0, n = uplinks.size(); i < n; i += 1) {
        final U u = uplinks.get(i);
        if (u.isConnected()) {
          u.sendDown(body);
        } else {
          closeUplink(u.linkKey());
        }
      }
    } while (uplinks != this.uplinks);
  }

  @Override
  public void pushUp(PushRequest pushRequest) {
    final Envelope envelope = pushRequest.envelope();
    if (envelope instanceof CommandMessage) {
      onCommand((CommandMessage) envelope);
      pushRequest.didDeliver();
    } else {
      pushRequest.didDecline();
    }
  }

  @Override
  public void pushUpCommand(CommandMessage message) {
    onCommand(message);
  }

  protected void onCommand(CommandMessage message) {
    new LaneRelayOnCommand<View>(this, message).run();
  }

  protected abstract void didOpenLaneView(View view);

  protected void didCloseLaneView(View view) {
    // stub
  }

  protected void didUplink(U uplink) {
    new LaneRelayDidUplink<View>(this, uplink).run();
  }

  protected void didEnter(Identity identity) {
    new LaneRelayDidEnter<View>(this, identity).run();
  }

  protected void didLeave(Identity identity) {
    new LaneRelayDidLeave<View>(this, identity).run();
  }

  public Decoder<Object> decodeRequest(HttpLaneUplink uplink, HttpRequest<?> request) {
    final Object views = this.views;
    LaneView view;
    Decoder<Object> decoder = null;
    if (views instanceof LaneView) {
      view = (LaneView) views;
      decoder = view.dispatchDecodeRequest(uplink, request);
      if (decoder == null) {
        decoder = view.laneDecodeRequest(uplink, request);
      }
    } else if (views instanceof LaneView[]) {
      final LaneView[] viewArray = (LaneView[]) views;
      for (int i = 0, n = viewArray.length; i < n; i += 1) {
        view = viewArray[i];
        decoder = view.dispatchDecodeRequest(uplink, request);
        if (decoder == null) {
          decoder = view.laneDecodeRequest(uplink, request);
        }
        if (decoder != null) {
          break;
        }
      }
    }
    return decoder;
  }

  public void willRequest(HttpLaneUplink uplink, HttpRequest<?> request) {
    new LaneRelayWillRequestHttp<View>(this, uplink, request).run();
  }

  public void didRequest(HttpLaneUplink uplink, HttpRequest<Object> request) {
    new LaneRelayDidRequestHttp<View>(this, uplink, request).run();
  }

  public void doRespond(HttpLaneUplink uplink, HttpRequest<Object> request) {
    new LaneRelayDoRespondHttp<View>(this, uplink, request).run();
  }

  public void willRespond(HttpLaneUplink uplink, HttpResponse<?> response) {
    new LaneRelayWillRespondHttp<View>(this, uplink, response).run();
  }

  public void didRespond(HttpLaneUplink uplink, HttpResponse<?> response) {
    new LaneRelayDidRespondHttp<View>(this, uplink, response).run();
  }

  @Override
  public Iterator<DataBinding> dataBindings() {
    return Collections.emptyIterator();
  }

  @Override
  public void closeData(Value name) {
    // nop
  }

  @Override
  public ListDataBinding openListData(Value name) {
    return this.laneContext.openListData(name);
  }

  @Override
  public ListDataBinding injectListData(ListDataBinding dataBinding) {
    return this.laneContext.injectListData(dataBinding);
  }

  @Override
  public MapDataBinding openMapData(Value name) {
    return this.laneContext.openMapData(name);
  }

  @Override
  public MapDataBinding injectMapData(MapDataBinding dataBinding) {
    return this.laneContext.injectMapData(dataBinding);
  }

  @Override
  public <S> SpatialDataBinding<S> openSpatialData(Value name, Z2Form<S> shapeForm) {
    return this.laneContext.openSpatialData(name, shapeForm);
  }

  @Override
  public <S> SpatialDataBinding<S> injectSpatialData(SpatialDataBinding<S> dataBinding) {
    return this.laneContext.injectSpatialData(dataBinding);
  }

  @Override
  public ValueDataBinding openValueData(Value name) {
    return this.laneContext.openValueData(name);
  }

  @Override
  public ValueDataBinding injectValueData(ValueDataBinding dataBinding) {
    return this.laneContext.injectValueData(dataBinding);
  }

  @Override
  public LinkBinding bindDownlink(Downlink downlink) {
    return this.laneContext.bindDownlink(downlink);
  }

  @Override
  public void openDownlink(LinkBinding link) {
    this.laneContext.openDownlink(link);
  }

  @Override
  public void closeDownlink(LinkBinding link) {
    this.laneContext.closeDownlink(link);
  }

  @Override
  public void httpDownlink(HttpBinding http) {
    this.laneContext.httpDownlink(http);
  }

  @Override
  public void pushDown(PushRequest pushRequest) {
    this.laneContext.pushDown(pushRequest);
  }

  @Override
  public void trace(Object message) {
    this.laneContext.trace(message);
  }

  @Override
  public void debug(Object message) {
    this.laneContext.debug(message);
  }

  @Override
  public void info(Object message) {
    this.laneContext.info(message);
  }

  @Override
  public void warn(Object message) {
    this.laneContext.warn(message);
  }

  @Override
  public void error(Object message) {
    this.laneContext.error(message);
  }

  @Override
  protected void willOpen() {
    super.willOpen();
  }

  @Override
  protected void willLoad() {
    super.willLoad();
  }

  @Override
  protected void willStart() {
    super.willStart();
  }

  @Override
  protected void willStop() {
    super.willStop();
  }

  @Override
  protected void willUnload() {
    super.willUnload();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void willClose() {
    super.willClose();
    final FingerTrieSeq<U> uplinks = (FingerTrieSeq<U>) (FingerTrieSeq<?>) this.uplinks;
    for (int i = 0, n = uplinks.size(); i < n; i += 1) {
      uplinks.get(i).close();
    }
    this.laneContext.close();
  }

  @Override
  public void didClose() {
  }

  @Override
  public void didFail(Throwable error) {
    error.printStackTrace();
  }

  @SuppressWarnings("unchecked")
  static final AtomicReferenceFieldUpdater<LaneModel<? extends LaneView, ? extends UplinkModem>, Object> VIEWS =
      AtomicReferenceFieldUpdater.newUpdater((Class<LaneModel<? extends LaneView, ? extends UplinkModem>>) (Class<?>) LaneModel.class, Object.class, "views");

  @SuppressWarnings("unchecked")
  static final AtomicReferenceFieldUpdater<LaneModel<? extends LaneView, ? extends UplinkModem>, FingerTrieSeq<? extends UplinkModem>> UPLINKS =
      AtomicReferenceFieldUpdater.newUpdater((Class<LaneModel<? extends LaneView, ? extends UplinkModem>>) (Class<?>) LaneModel.class, (Class<FingerTrieSeq<? extends UplinkModem>>) (Class<?>) FingerTrieSeq.class, "uplinks");
}

final class LaneRelayOnCommand<View extends LaneView> extends LaneRelay<LaneModel<View, ?>, View> {
  final CommandMessage message;

  LaneRelayOnCommand(LaneModel<View, ?> model, CommandMessage message) {
    super(model, 2);
    this.message = message;
  }

  @Override
  boolean runPhase(View view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneWillCommand(this.message);
      }
      return view.dispatchWillCommand(this.message.body(), preemptive);
    } else if (phase == 1) {
      if (preemptive) {
        view.laneDidCommand(this.message);
      }
      return view.dispatchDidCommand(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }
}

final class LaneRelayDidUplink<View extends LaneView> extends LaneRelay<LaneModel<View, ?>, View> {
  final Uplink uplink;

  LaneRelayDidUplink(LaneModel<View, ?> model, Uplink uplink) {
    super(model);
    this.uplink = uplink;
  }

  @Override
  boolean runPhase(View view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneDidUplink(this.uplink);
      }
      return view.dispatchDidUplink(this.uplink, preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }
}

final class LaneRelayDidEnter<View extends LaneView> extends LaneRelay<LaneModel<View, ?>, View> {
  final Identity identity;

  LaneRelayDidEnter(LaneModel<View, ?> model, Identity identity) {
    super(model);
    this.identity = identity;
  }

  @Override
  boolean runPhase(View view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneDidEnter(this.identity);
      }
      return view.dispatchDidEnter(this.identity, preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }
}

final class LaneRelayDidLeave<View extends LaneView> extends LaneRelay<LaneModel<View, ?>, View> {
  final Identity identity;

  LaneRelayDidLeave(LaneModel<View, ?> model, Identity identity) {
    super(model);
    this.identity = identity;
  }

  @Override
  boolean runPhase(View view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneDidLeave(this.identity);
      }
      return view.dispatchDidLeave(this.identity, preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }
}

final class LaneRelayWillRequestHttp<View extends LaneView> extends LaneRelay<LaneModel<View, ?>, View> {
  final HttpLaneUplink uplink;
  final HttpRequest<?> request;

  LaneRelayWillRequestHttp(LaneModel<View, ?> model, HttpLaneUplink uplink, HttpRequest<?> request) {
    super(model);
    this.uplink = uplink;
    this.request = request;
  }

  @Override
  boolean runPhase(View view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneWillRequest(this.uplink, this.request);
      }
      return view.dispatchWillRequest(this.uplink, this.request, preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }
}

final class LaneRelayDidRequestHttp<View extends LaneView> extends LaneRelay<LaneModel<View, ?>, View> {
  final HttpLaneUplink uplink;
  final HttpRequest<Object> request;

  LaneRelayDidRequestHttp(LaneModel<View, ?> model, HttpLaneUplink uplink, HttpRequest<Object> request) {
    super(model);
    this.uplink = uplink;
    this.request = request;
  }

  @Override
  boolean runPhase(View view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneDidRequest(this.uplink, this.request);
      }
      return view.dispatchDidRequest(this.uplink, this.request, preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }
}

final class LaneRelayDoRespondHttp<View extends LaneView> extends LaneRelay<LaneModel<View, ?>, View> {
  final HttpLaneUplink uplink;
  final HttpRequest<Object> request;
  HttpResponse<?> response;

  LaneRelayDoRespondHttp(LaneModel<View, ?> model, HttpLaneUplink uplink, HttpRequest<Object> request) {
    super(model);
    this.uplink = uplink;
    this.request = request;
  }

  @Override
  boolean runPhase(View view, int phase, boolean preemptive) {
    if (phase == 0) {
      Object response = this.response;
      if (response == null) {
        response = view.dispatchDoRespond(this.uplink, this.request, preemptive);
      }
      final boolean complete = response == Boolean.TRUE;
      if (complete) {
        response = view.laneDoRespond(this.uplink, this.request);
      }
      if (response instanceof HttpResponse<?>) {
        this.response = (HttpResponse<?>) response;
        return true;
      }
      return complete;
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    if (this.response == null) {
      this.response = HttpResponse.from(HttpStatus.NOT_FOUND).entity(HttpBody.empty());
    }
    this.uplink.writeResponse(this.response);
  }
}

final class LaneRelayWillRespondHttp<View extends LaneView> extends LaneRelay<LaneModel<View, ?>, View> {
  final HttpLaneUplink uplink;
  final HttpResponse<?> response;

  LaneRelayWillRespondHttp(LaneModel<View, ?> model, HttpLaneUplink uplink, HttpResponse<?> response) {
    super(model);
    this.uplink = uplink;
    this.response = response;
  }

  @Override
  boolean runPhase(View view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneWillRespond(this.uplink, this.response);
      }
      return view.dispatchWillRespond(this.uplink, this.response, preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }
}

final class LaneRelayDidRespondHttp<View extends LaneView> extends LaneRelay<LaneModel<View, ?>, View> {
  final HttpLaneUplink uplink;
  final HttpResponse<?> response;

  LaneRelayDidRespondHttp(LaneModel<View, ?> model, HttpLaneUplink uplink, HttpResponse<?> response) {
    super(model);
    this.uplink = uplink;
    this.response = response;
  }

  @Override
  boolean runPhase(View view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneDidRespond(this.uplink, this.response);
      }
      return view.dispatchDidRespond(this.uplink, this.response, preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }
}
