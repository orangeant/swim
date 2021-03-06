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

import java.util.Iterator;
import java.util.Map;
import swim.api.Link;
import swim.api.data.SpatialData;
import swim.collections.FingerTrieSeq;
import swim.math.Z2Form;
import swim.runtime.LaneContext;
import swim.runtime.LinkBinding;
import swim.spatial.SpatialMap;
import swim.structure.Form;
import swim.structure.Record;
import swim.structure.Value;
import swim.warp.CommandMessage;

public class SpatialLaneModel<S> extends LaneModel<SpatialLaneView<?, S, ?>, SpatialLaneUplink<S>> {

  protected int flags;
  protected SpatialData<Value, S, Value> data;
  protected final Z2Form<S> shapeForm;

  SpatialLaneModel(Z2Form<S> shapeForm, int flags) {
    this.shapeForm = shapeForm;
    this.flags = flags;
  }

  public SpatialLaneModel(Z2Form<S> shapeForm) {
    this(shapeForm, 0);
  }

  @Override
  protected SpatialLaneUplink<S> createUplink(LinkBinding link) {
    return new SpatialLaneUplink<S>(this, link);
  }

  @Override
  public void setLaneContext(LaneContext laneContext) {
    super.setLaneContext(laneContext);
    openStore();
  }

  protected void openStore() {
    this.data = this.laneContext.data().spatialData(laneUri().toString(), shapeForm)
        .isResident(isResident())
        .isTransient(isTransient());
  }

  @Override
  public void onCommand(CommandMessage message) {
    final Value payload = message.body();
    final String tag = payload.tag();
    if ("update".equals(tag)) {
      final Value header = payload.header("update");
      final Value key = header.get("key");
      final Value shape = header.get("shape");
      final S shapeObject = shapeForm.cast(shape);
      final Value value = payload.body();
      new SpatialLaneRelayUpdate<S>(this, null, message, key, shapeObject, value).run();
    } else if ("move".equals(tag)) {
      final Value header = payload.header("move");
      final Value key = header.get("key");
      final Value oldShape = header.get("from");
      final Value newShape = header.get("to");
      final S oldShapeObject = shapeForm.cast(oldShape);
      final S newShapeObject = shapeForm.cast(newShape);
      final Value value = payload.body();
      new SpatialLaneRelayMove<S>(this, null, message, key, oldShapeObject, newShapeObject, value).run();
    } else if ("remove".equals(tag)) {
      final Value header = payload.header("remove");
      final Value key = header.get("key");
      final Value shape = header.get("shape");
      final S shapeObject = shapeForm.cast(shape);
      new SpatialLaneRelayRemove<S>(this, null, message, key, shapeObject).run();
    } else if ("clear".equals(tag)) {
      new SpatialLaneRelayClear<S>(this, null, message).run();
    }
  }

  protected void cueDownKey(Value key) {
    FingerTrieSeq<SpatialLaneUplink<S>> uplinks;
    do {
      uplinks = this.uplinks;
      for (int i = 0, n = uplinks.size(); i < n; i += 1) {
        uplinks.get(i).cueDownKey(key);
      }
    } while (uplinks != this.uplinks);
  }

  @Override
  protected void didOpenLaneView(SpatialLaneView<?, S, ?> view) {
    view.setLaneBinding(this);
  }

  public final boolean isResident() {
    return (this.flags & RESIDENT) != 0;
  }

  public SpatialLaneModel<S> isResident(boolean isResident) {
    if (this.data != null) {
      this.data.isResident(isResident);
    }
    if (isResident) {
      this.flags |= RESIDENT;
    } else {
      this.flags &= ~RESIDENT;
    }
    final Object views = this.views;
    if (views instanceof SpatialLaneView<?, ?, ?>) {
      ((SpatialLaneView<?, ?, ?>) views).didSetResident(isResident);
    } else if (views instanceof LaneView[]) {
      final LaneView[] viewArray = (LaneView[]) views;
      for (int i = 0, n = viewArray.length; i < n; i += 1) {
        ((SpatialLaneView<?, ?, ?>) viewArray[i]).didSetResident(isResident);
      }
    }
    return this;
  }

  public final boolean isTransient() {
    return (this.flags & TRANSIENT) != 0;
  }

  public SpatialLaneModel<S> isTransient(boolean isTransient) {
    if (this.data != null) {
      this.data.isTransient(isTransient);
    }
    if (isTransient) {
      this.flags |= TRANSIENT;
    } else {
      this.flags &= ~TRANSIENT;
    }
    final Object views = this.views;
    if (views instanceof SpatialLaneView<?, ?, ?>) {
      ((SpatialLaneView<?, ?, ?>) views).didSetTransient(isTransient);
    } else if (views instanceof LaneView[]) {
      final LaneView[] viewArray = (LaneView[]) views;
      for (int i = 0, n = viewArray.length; i < n; i += 1) {
        ((SpatialLaneView<?, ?, ?>) viewArray[i]).didSetTransient(isTransient);
      }
    }
    return this;
  }

  public final boolean isSigned() {
    return (this.flags & SIGNED) != 0;
  }

  public SpatialLaneModel<S> isSigned(boolean isSigned) {
    if (isSigned) {
      this.flags |= SIGNED;
    } else {
      this.flags &= ~SIGNED;
    }
    final Object views = this.views;
    if (views instanceof SpatialLaneView<?, ?, ?>) {
      ((SpatialLaneView<?, ?, ?>) views).didSetSigned(isSigned);
    } else if (views instanceof LaneView[]) {
      final LaneView[] viewArray = (LaneView[]) views;
      for (int i = 0, n = viewArray.length; i < n; i += 1) {
        ((SpatialLaneView<?, ?, ?>) viewArray[i]).didSetSigned(isSigned);
      }
    }
    return this;
  }

  public Value get(Value key) {
    if (key != null) {
      return this.data.get(key);
    } else {
      return Value.absent();
    }
  }

  @SuppressWarnings("unchecked")
  public <K, V> V put(SpatialLaneView<K, S, V> view, K keyObject, S shapeObject, V newObject) {
    final Form<K> keyForm = view.keyForm;
    final Form<V> valueForm = view.valueForm;
    final Value key = keyForm.mold(keyObject).toValue();
    final Value newValue = valueForm.mold(newObject).toValue();
    final SpatialLaneRelayUpdate<S> relay = new SpatialLaneRelayUpdate<S>(this, null, key, shapeObject, newValue);
    relay.keyForm = (Form<Object>) keyForm;
    relay.shapeForm = (Z2Form<Object>) shapeForm;
    relay.valueForm = (Form<Object>) valueForm;
    relay.keyObject = keyObject;
    relay.oldObject = newObject;
    relay.newObject = newObject;
    relay.stage = stage();
    relay.run();
    if (relay.valueForm != valueForm && valueForm != null) {
      relay.oldObject = valueForm.cast(relay.oldValue);
      if (relay.oldObject == null) {
        relay.oldObject = valueForm.unit();
      }
    }
    return (V) relay.oldObject;
  }

  @SuppressWarnings("unchecked")
  public <K, V> V move(SpatialLaneView<K, S, V> view, K keyObject,
                       S oldShapeObject, S newShapeObject, V newObject) {
    final Form<K> keyForm = view.keyForm;
    final Form<V> valueForm = view.valueForm;
    final Value key = keyForm.mold(keyObject).toValue();
    final Value newValue = valueForm.mold(newObject).toValue();
    final SpatialLaneRelayMove<S> relay =
        new SpatialLaneRelayMove<S>(this, null, key, oldShapeObject, newShapeObject, newValue);
    relay.keyForm = (Form<Object>) keyForm;
    relay.shapeForm = (Z2Form<Object>) shapeForm;
    relay.valueForm = (Form<Object>) valueForm;
    relay.keyObject = keyObject;
    relay.oldObject = newObject;
    relay.newObject = newObject;
    relay.stage = stage();
    relay.run();
    if (relay.valueForm != valueForm && valueForm != null) {
      relay.oldObject = valueForm.cast(relay.oldValue);
      if (relay.oldObject == null) {
        relay.oldObject = valueForm.unit();
      }
    }
    return (V) relay.oldObject;
  }

  @SuppressWarnings("unchecked")
  public <K, V> V remove(SpatialLaneView<K, S, V> view, K keyObject, S shapeObject) {
    final Form<K> keyForm = view.keyForm;
    final Form<V> valueForm = view.valueForm;
    final Value key = keyForm.mold(keyObject).toValue();
    final SpatialLaneRelayRemove<S> relay = new SpatialLaneRelayRemove<S>(this, null, key, shapeObject);
    relay.keyForm = (Form<Object>) keyForm;
    relay.shapeForm = (Z2Form<Object>) shapeForm;
    relay.valueForm = (Form<Object>) valueForm;
    relay.keyObject = keyObject;
    relay.stage = stage();
    relay.run();
    if (relay.valueForm != valueForm && valueForm != null) {
      relay.oldObject = valueForm.cast(relay.oldValue);
      if (relay.oldObject == null) {
        relay.oldObject = valueForm.unit();
      }
    }
    return (V) relay.oldObject;
  }

  public void clear(SpatialLaneView<?, S, ?> view) {
    final SpatialLaneRelayClear<S> relay = new SpatialLaneRelayClear<S>(this, null);
    relay.stage = stage();
    relay.run();
  }

  public Iterator<SpatialMap.Entry<Value, S, Value>> iterator(S shape) {
    return this.data.iterator(shape);
  }

  public Iterator<SpatialMap.Entry<Value, S, Value>> iterator() {
    return this.data.iterator();
  }

  static final int RESIDENT = 1 << 0;
  static final int TRANSIENT = 1 << 1;
  static final int SIGNED = 1 << 2;

}

final class SpatialLaneRelayUpdate<S> extends LaneRelay<SpatialLaneModel<S>, SpatialLaneView<?, S, ?>> {
  final Link link;
  final CommandMessage message;
  Form<Object> keyForm;
  Z2Form<Object> shapeForm;
  Form<Object> valueForm;
  final Value key;
  Object keyObject;
  final S shapeObject;
  Value oldValue;
  Object oldObject;
  Value newValue;
  Object newObject;

  SpatialLaneRelayUpdate(SpatialLaneModel<S> model, Link link, CommandMessage message, Value key, S shapeObject,
                         Value newValue) {
    super(model, 4);
    this.link = link;
    this.message = message;
    this.key = key;
    this.shapeObject = shapeObject;
    this.newValue = newValue;
  }

  SpatialLaneRelayUpdate(SpatialLaneModel<S> model, Link link, Value key, S shapeObject, Value newValue) {
    super(model, 1, 3);
    this.link = link;
    this.message = null;
    this.key = key;
    this.shapeObject = shapeObject;
    this.newValue = newValue;
  }

  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      this.oldValue = model.data.put(key, shapeObject, newValue);
      if (this.valueForm != null) {
        this.oldObject = this.valueForm.cast(this.oldValue);
        if (this.oldObject == null) {
          this.oldObject = this.valueForm.unit();
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  boolean runPhase(SpatialLaneView<?, S, ?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneWillCommand(this.message);
      }
      return view.dispatchWillCommand(this.message.body(), preemptive);
    } else if (phase == 1) {
      final Form<Object> keyForm = (Form<Object>) view.keyForm;
      if (this.keyForm != keyForm && keyForm != null) {
        this.keyForm = keyForm;
        this.keyObject = keyForm.cast(this.key);
        if (this.keyObject == null) {
          this.keyObject = keyForm.unit();
        }
      }
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      if (this.valueForm != valueForm && valueForm != null) {
        this.valueForm = valueForm;
        this.oldObject = valueForm.cast(this.newValue);
        if (this.oldObject == null) {
          this.oldObject = valueForm.unit();
        }
      }
      if (preemptive) {
        this.newObject = ((SpatialLaneView<Object, S, Object>) view)
            .laneWillUpdate(this.keyObject, this.shapeObject, this.oldObject);
      }
      final Map.Entry<Boolean, Object> result = ((SpatialLaneView<Object, S, Object>) view)
          .dispatchWillUpdate(this.link, this.keyObject, this.shapeObject, this.oldObject, preemptive);
      this.newObject = result.getValue();
      if (this.oldObject != this.newObject) {
        this.oldObject = this.newObject;
        this.newValue = valueForm.mold(this.newObject).toValue();
        this.oldValue = this.newValue;
      }
      return result.getKey();
    } else if (phase == 2) {
      final Form<Object> keyForm = (Form<Object>) view.keyForm;
      if (this.keyForm != keyForm && keyForm != null) {
        this.keyForm = keyForm;
        this.keyObject = keyForm.cast(this.key);
        if (this.keyObject == null) {
          this.keyObject = keyForm.unit();
        }
      }
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      if (this.valueForm != valueForm && valueForm != null) {
        this.valueForm = valueForm;
        this.oldObject = valueForm.cast(this.oldValue);
        if (this.oldObject == null) {
          this.oldObject = valueForm.unit();
        }
        this.newObject = valueForm.cast(this.newValue);
        if (this.newObject == null) {
          this.newObject = valueForm.unit();
        }
      }
      if (preemptive) {
        ((SpatialLaneView<Object, S, Object>) view)
            .laneDidUpdate(this.keyObject, this.shapeObject, this.newObject, this.oldObject);
      }
      return ((SpatialLaneView<Object, S, Object>) view)
          .dispatchDidUpdate(this.link, this.keyObject, this.shapeObject, this.newObject, this.oldObject, preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.laneDidCommand(this.message);
      }
      return view.dispatchDidCommand(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    this.model.cueDownKey(this.key);
  }
}

final class SpatialLaneRelayMove<S> extends LaneRelay<SpatialLaneModel<S>, SpatialLaneView<?, S, ?>> {
  final Link link;
  final CommandMessage message;
  Form<Object> keyForm;
  Z2Form<Object> shapeForm;
  Form<Object> valueForm;
  final Value key;
  Object keyObject;
  final S oldShapeObject;
  final S newShapeObject;
  Value oldValue;
  Object oldObject;
  Value newValue;
  Object newObject;

  SpatialLaneRelayMove(SpatialLaneModel<S> model, Link link, CommandMessage message, Value key, S oldShapeObject,
                       S newShapeObject, Value newValue) {
    super(model, 4);
    this.link = link;
    this.message = message;
    this.key = key;
    this.oldShapeObject = oldShapeObject;
    this.newShapeObject = newShapeObject;
    this.newValue = newValue;
  }

  SpatialLaneRelayMove(SpatialLaneModel<S> model, Link link, Value key, S oldShapeObject,
                       S newShapeObject, Value newValue) {
    super(model, 1, 3);
    this.link = link;
    this.message = null;
    this.key = key;
    this.oldShapeObject = oldShapeObject;
    this.newShapeObject = newShapeObject;
    this.newValue = newValue;
  }

  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      this.oldValue = model.data.move(key, oldShapeObject, newShapeObject, newValue);
      if (this.valueForm != null) {
        this.oldObject = this.valueForm.cast(this.oldValue);
        if (this.oldObject == null) {
          this.oldObject = this.valueForm.unit();
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  boolean runPhase(SpatialLaneView<?, S, ?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneWillCommand(this.message);
      }
      return view.dispatchWillCommand(this.message.body(), preemptive);
    } else if (phase == 1) {
      final Form<Object> keyForm = (Form<Object>) view.keyForm;
      if (this.keyForm != keyForm && keyForm != null) {
        this.keyForm = keyForm;
        this.keyObject = keyForm.cast(this.key);
        if (this.keyObject == null) {
          this.keyObject = keyForm.unit();
        }
      }
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      if (this.valueForm != valueForm && valueForm != null) {
        this.valueForm = valueForm;
        this.oldObject = valueForm.cast(this.newValue);
        if (this.oldObject == null) {
          this.oldObject = valueForm.unit();
        }
      }
      if (preemptive) {
        this.newObject = ((SpatialLaneView<Object, S, Object>) view)
            .laneWillMove(this.keyObject, this.newShapeObject, this.oldObject, this.oldShapeObject);
      }
      final Map.Entry<Boolean, Object> result = ((SpatialLaneView<Object, S, Object>) view)
          .dispatchWillMove(this.link, this.keyObject, this.newShapeObject, this.oldObject, this.oldShapeObject, preemptive);
      this.newObject = result.getValue();
      if (this.oldObject != this.newObject) {
        this.oldObject = this.newObject;
        this.newValue = valueForm.mold(this.newObject).toValue();
        this.oldValue = this.newValue;
      }
      return result.getKey();
    } else if (phase == 2) {
      final Form<Object> keyForm = (Form<Object>) view.keyForm;
      if (this.keyForm != keyForm && keyForm != null) {
        this.keyForm = keyForm;
        this.keyObject = keyForm.cast(this.key);
        if (this.keyObject == null) {
          this.keyObject = keyForm.unit();
        }
      }
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      if (this.valueForm != valueForm && valueForm != null) {
        this.valueForm = valueForm;
        this.oldObject = valueForm.cast(this.oldValue);
        if (this.oldObject == null) {
          this.oldObject = valueForm.unit();
        }
        this.newObject = valueForm.cast(this.newValue);
        if (this.newObject == null) {
          this.newObject = valueForm.unit();
        }
      }
      if (preemptive) {
        ((SpatialLaneView<Object, S, Object>) view)
            .laneDidMove(this.keyObject, this.newShapeObject, this.newObject, this.oldShapeObject, this.oldObject);
      }
      return ((SpatialLaneView<Object, S, Object>) view)
          .dispatchDidMove(this.link, this.keyObject, this.newShapeObject, this.newObject, this.oldShapeObject,
              this.oldObject, preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.laneDidCommand(this.message);
      }
      return view.dispatchDidCommand(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    final Record header = Record.create(3).slot("key", key).slot("from", shapeForm.mold(this.oldShapeObject).toValue())
        .slot("to", shapeForm.mold(this.newShapeObject).toValue());
    this.model.sendDown(Record.create(1).attr("move", header));
  }
}

final class SpatialLaneRelayRemove<S> extends LaneRelay<SpatialLaneModel<S>, SpatialLaneView<?, S, ?>> {
  final Link link;
  final CommandMessage message;
  Form<Object> keyForm;
  Z2Form<Object> shapeForm;
  Form<Object> valueForm;
  final Value key;
  Object keyObject;
  final S shapeObject;
  Value oldValue;
  Object oldObject;

  SpatialLaneRelayRemove(SpatialLaneModel<S> model, Link link, CommandMessage message, Value key, S shapeObject) {
    super(model, 4);
    this.link = link;
    this.message = message;
    this.key = key;
    this.shapeObject = shapeObject;
  }

  SpatialLaneRelayRemove(SpatialLaneModel<S> model, Link link, Value key, S shapeObject) {
    super(model, 1, 3);
    this.link = link;
    this.message = null;
    this.key = key;
    this.shapeObject = shapeObject;
  }

  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      this.oldValue = model.data.remove(key, shapeObject);
      if (this.valueForm != null) {
        this.oldObject = this.valueForm.cast(this.oldValue);
        if (this.oldObject == null) {
          this.oldObject = this.valueForm.unit();
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  boolean runPhase(SpatialLaneView<?, S, ?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneWillCommand(this.message);
      }
      return view.dispatchWillCommand(this.message.body(), preemptive);
    } else if (phase == 1) {
      final Form<Object> keyForm = (Form<Object>) view.keyForm;
      if (this.keyForm != keyForm && keyForm != null) {
        this.keyForm = keyForm;
        this.keyObject = keyForm.cast(this.key);
        if (this.keyObject == null) {
          this.keyObject = keyForm.unit();
        }
      }
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      if (this.valueForm != valueForm && valueForm != null) {
        this.valueForm = valueForm;
        this.oldObject = valueForm.cast(this.oldValue);
        if (this.oldObject == null) {
          this.oldObject = valueForm.unit();
        }
      }
      if (preemptive) {
        ((SpatialLaneView<Object, S, Object>) view).laneWillRemove(this.keyObject, this.shapeObject);
      }
      return ((SpatialLaneView<Object, S, Object>) view)
          .dispatchWillRemove(this.link, this.keyObject, this.shapeObject, preemptive);
    } else if (phase == 2) {
      final Form<Object> keyForm = (Form<Object>) view.keyForm;
      if (this.keyForm != keyForm && keyForm != null) {
        this.keyForm = keyForm;
        this.keyObject = keyForm.cast(this.key);
        if (this.keyObject == null) {
          this.keyObject = keyForm.unit();
        }
      }
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      if (this.valueForm != valueForm && valueForm != null) {
        this.valueForm = valueForm;
        this.oldObject = valueForm.cast(this.oldValue);
        if (this.oldObject == null) {
          this.oldObject = valueForm.unit();
        }
      }
      if (preemptive) {
        ((SpatialLaneView<Object, S, Object>) view).laneDidRemove(this.keyObject, this.shapeObject);
      }
      return ((SpatialLaneView<Object, S, Object>) view)
          .dispatchDidRemove(this.link, this.keyObject, this.shapeObject, this.oldObject, preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.laneDidCommand(this.message);
      }
      return view.dispatchDidCommand(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    final Record header = Record.create(2).slot("key", key).slot("shape", shapeForm.mold(this.shapeObject).toValue());
    this.model.sendDown(Record.create(1).attr("move", header));
  }
}

final class SpatialLaneRelayClear<S> extends LaneRelay<SpatialLaneModel<S>, SpatialLaneView<?, S, ?>> {
  final Link link;
  final CommandMessage message;

  SpatialLaneRelayClear(SpatialLaneModel<S> model, Link link, CommandMessage message) {
    super(model, 4);
    this.link = link;
    this.message = message;
  }

  SpatialLaneRelayClear(SpatialLaneModel<S> model, Link link) {
    super(model, 1, 3);
    this.link = link;
    this.message = null;
  }

  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      this.model.data.clear();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  boolean runPhase(SpatialLaneView<?, S, ?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.laneWillCommand(this.message);
      }
      return view.dispatchWillCommand(this.message.body(), preemptive);
    } else if (phase == 1) {
      if (preemptive) {
        view.laneWillClear();
      }
      return view.dispatchWillClear(this.link, preemptive);
    } else if (phase == 2) {
      if (preemptive) {
        view.laneDidClear();
      }
      return view.dispatchDidClear(this.link, preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.laneDidCommand(this.message);
      }
      return view.dispatchDidCommand(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    this.model.sendDown(Record.create(1).attr("clear"));
  }
}
