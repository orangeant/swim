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

package swim.runtime;

import java.util.Iterator;
import swim.api.auth.Credentials;
import swim.api.auth.Identity;
import swim.api.data.DataFactory;
import swim.api.downlink.Downlink;
import swim.api.policy.Policy;
import swim.api.policy.PolicyDirective;
import swim.collections.FingerTrieSeq;
import swim.concurrent.Schedule;
import swim.concurrent.Stage;
import swim.math.Z2Form;
import swim.store.DataBinding;
import swim.store.ListDataBinding;
import swim.store.MapDataBinding;
import swim.store.SpatialDataBinding;
import swim.store.ValueDataBinding;
import swim.structure.Value;
import swim.uri.Uri;

public class MeshProxy implements MeshBinding, MeshContext {
  protected final MeshBinding meshBinding;
  protected MeshContext meshContext;

  public MeshProxy(MeshBinding meshBinding) {
    this.meshBinding = meshBinding;
  }

  public final MeshBinding meshBinding() {
    return this.meshBinding;
  }

  @Override
  public final MeshContext meshContext() {
    return this.meshContext;
  }

  @Override
  public void setMeshContext(MeshContext meshContext) {
    this.meshContext = meshContext;
    this.meshBinding.setMeshContext(this);
  }

  @Override
  public final TierContext tierContext() {
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T unwrapMesh(Class<T> meshClass) {
    if (meshClass.isAssignableFrom(getClass())) {
      return (T) this;
    } else {
      return this.meshBinding.unwrapMesh(meshClass);
    }
  }

  @Override
  public Uri meshUri() {
    return this.meshContext.meshUri();
  }

  @Override
  public Policy policy() {
    return this.meshContext.policy();
  }

  @Override
  public Schedule schedule() {
    return this.meshContext.schedule();
  }

  @Override
  public Stage stage() {
    return this.meshContext.stage();
  }

  @Override
  public DataFactory data() {
    return this.meshContext.data();
  }

  @Override
  public PartBinding getGateway() {
    return this.meshBinding.getGateway();
  }

  @Override
  public void setGateway(PartBinding gateway) {
    this.meshBinding.setGateway(gateway);
  }

  @Override
  public PartBinding getOurself() {
    return this.meshBinding.getOurself();
  }

  @Override
  public void setOurself(PartBinding ourself) {
    this.meshBinding.setOurself(ourself);
  }

  @Override
  public FingerTrieSeq<PartBinding> getParts() {
    return this.meshBinding.getParts();
  }

  @Override
  public PartBinding getPart(Uri nodeUri) {
    return this.meshBinding.getPart(nodeUri);
  }

  @Override
  public PartBinding getPart(Value partKey) {
    return this.meshBinding.getPart(partKey);
  }

  @Override
  public PartBinding openPart(Uri nodeUri) {
    return this.meshBinding.openPart(nodeUri);
  }

  @Override
  public PartBinding openGateway() {
    return this.meshBinding.openGateway();
  }

  @Override
  public PartBinding addPart(Value partKey, PartBinding part) {
    return this.meshBinding.addPart(partKey, part);
  }

  @Override
  public PartBinding createPart(Value partKey) {
    return this.meshContext.createPart(partKey);
  }

  @Override
  public PartBinding injectPart(Value partKey, PartBinding part) {
    return this.meshContext.injectPart(partKey, part);
  }

  @Override
  public HostBinding createHost(Value partKey, Uri hostUri) {
    return this.meshContext.createHost(partKey, hostUri);
  }

  @Override
  public HostBinding injectHost(Value partKey, Uri hostUri, HostBinding host) {
    return this.meshContext.injectHost(partKey, hostUri, host);
  }

  @Override
  public NodeBinding createNode(Value partKey, Uri hostUri, Uri nodeUri) {
    return this.meshContext.createNode(partKey, hostUri, nodeUri);
  }

  @Override
  public NodeBinding injectNode(Value partKey, Uri hostUri, Uri nodeUri, NodeBinding node) {
    return this.meshContext.injectNode(partKey, hostUri, nodeUri, node);
  }

  @Override
  public LaneBinding injectLane(Value partKey, Uri hostUri, Uri nodeUri, Uri laneUri, LaneBinding lane) {
    return this.meshContext.injectLane(partKey, hostUri, nodeUri, laneUri, lane);
  }

  @Override
  public PolicyDirective<Identity> authenticate(Credentials credentials) {
    return this.meshContext.authenticate(credentials);
  }

  @Override
  public Iterator<DataBinding> dataBindings() {
    return this.meshBinding.dataBindings();
  }

  @Override
  public void closeData(Value name) {
    this.meshBinding.closeData(name);
  }

  @Override
  public ListDataBinding openListData(Value name) {
    return this.meshContext.openListData(name);
  }

  @Override
  public ListDataBinding injectListData(ListDataBinding dataBinding) {
    return this.meshContext.injectListData(dataBinding);
  }

  @Override
  public MapDataBinding openMapData(Value name) {
    return this.meshContext.openMapData(name);
  }

  @Override
  public MapDataBinding injectMapData(MapDataBinding dataBinding) {
    return this.meshContext.injectMapData(dataBinding);
  }

  @Override
  public <S> SpatialDataBinding<S> openSpatialData(Value name, Z2Form<S> shapeForm) {
    return this.meshContext.openSpatialData(name, shapeForm);
  }

  @Override
  public <S> SpatialDataBinding<S> injectSpatialData(SpatialDataBinding<S> dataBinding) {
    return this.meshContext.injectSpatialData(dataBinding);
  }

  @Override
  public ValueDataBinding openValueData(Value name) {
    return this.meshContext.openValueData(name);
  }

  @Override
  public ValueDataBinding injectValueData(ValueDataBinding dataBinding) {
    return this.meshContext.injectValueData(dataBinding);
  }

  @Override
  public LinkBinding bindDownlink(Downlink downlink) {
    return this.meshContext.bindDownlink(downlink);
  }

  @Override
  public void openDownlink(LinkBinding link) {
    this.meshContext.openDownlink(link);
  }

  @Override
  public void closeDownlink(LinkBinding link) {
    this.meshContext.closeDownlink(link);
  }

  @Override
  public void httpDownlink(HttpBinding http) {
    this.meshContext.httpDownlink(http);
  }

  @Override
  public void pushDown(PushRequest pushRequest) {
    this.meshContext.pushDown(pushRequest);
  }

  @Override
  public void openUplink(LinkBinding link) {
    this.meshBinding.openUplink(link);
  }

  @Override
  public void httpUplink(HttpBinding http) {
    this.meshBinding.httpUplink(http);
  }

  @Override
  public void pushUp(PushRequest pushRequest) {
    this.meshBinding.pushUp(pushRequest);
  }

  @Override
  public void trace(Object message) {
    this.meshContext.trace(message);
  }

  @Override
  public void debug(Object message) {
    this.meshContext.debug(message);
  }

  @Override
  public void info(Object message) {
    this.meshContext.info(message);
  }

  @Override
  public void warn(Object message) {
    this.meshContext.warn(message);
  }

  @Override
  public void error(Object message) {
    this.meshContext.error(message);
  }

  @Override
  public boolean isClosed() {
    return this.meshBinding.isClosed();
  }

  @Override
  public boolean isOpened() {
    return this.meshBinding.isOpened();
  }

  @Override
  public boolean isLoaded() {
    return this.meshBinding.isLoaded();
  }

  @Override
  public boolean isStarted() {
    return this.meshBinding.isStarted();
  }

  @Override
  public void open() {
    this.meshBinding.open();
  }

  @Override
  public void load() {
    this.meshBinding.load();
  }

  @Override
  public void start() {
    this.meshBinding.start();
  }

  @Override
  public void stop() {
    this.meshBinding.stop();
  }

  @Override
  public void unload() {
    this.meshBinding.unload();
  }

  @Override
  public void close() {
    this.meshBinding.close();
  }

  @Override
  public void willOpen() {
    this.meshContext.willOpen();
  }

  @Override
  public void didOpen() {
    this.meshContext.didOpen();
  }

  @Override
  public void willLoad() {
    this.meshContext.willLoad();
  }

  @Override
  public void didLoad() {
    this.meshContext.didLoad();
  }

  @Override
  public void willStart() {
    this.meshContext.willStart();
  }

  @Override
  public void didStart() {
    this.meshContext.didStart();
  }

  @Override
  public void willStop() {
    this.meshContext.willStop();
  }

  @Override
  public void didStop() {
    this.meshContext.didStop();
  }

  @Override
  public void willUnload() {
    this.meshContext.willUnload();
  }

  @Override
  public void didUnload() {
    this.meshContext.didUnload();
  }

  @Override
  public void willClose() {
    this.meshContext.willClose();
  }

  @Override
  public void didClose() {
    this.meshBinding.didClose();
  }

  @Override
  public void didFail(Throwable error) {
    this.meshBinding.didFail(error);
  }
}
