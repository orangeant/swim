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

package swim.runtime.downlink;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import swim.collections.STreeList;
import swim.structure.Attr;
import swim.structure.Form;
import swim.structure.Record;
import swim.structure.Value;
import swim.uri.Uri;
import swim.warp.EventMessage;

public class ListDownlinkModel extends SupplyDownlinkModem<ListDownlinkView<?>> {
  protected int flags;
  protected final STreeList<Value> state;

  public ListDownlinkModel(Uri meshUri, Uri hostUri, Uri nodeUri, Uri laneUri,
                           float prio, float rate, Value body) {
    super(meshUri, hostUri, nodeUri, laneUri, prio, rate, body);
    this.flags = 0;
    state = STreeList.empty();
  }

  public final boolean isStateful() {
    return (this.flags & STATEFUL) != 0;
  }

  public ListDownlinkModel isStateful(boolean isStateful) {
    if (isStateful) {
      this.flags |= STATEFUL;
    } else {
      this.flags &= ~STATEFUL;
    }
    final Object views = this.views;
    if (views instanceof DownlinkView) {
      ((ListDownlinkView<?>) views).didSetStateful(isStateful);
    } else if (views instanceof DownlinkView[]) {
      final DownlinkView[] viewArray = (DownlinkView[]) views;
      for (int i = 0, n = viewArray.length; i < n; i += 1) {
        ((ListDownlinkView<?>) viewArray[i]).didSetStateful(isStateful);
      }
    }
    return this;
  }

  @Override
  protected void pushDownEvent(EventMessage message) {
    onEvent(message);
    final Value payload = message.body();
    final String tag = payload.tag();
    if ("update".equals(tag)) {
      final Value header = payload.header("update");
      final int index = header.get("index").intValue(-1);
      if (index > -1) {
        final Object key;
        if (header.get("key").isDefined()) {
          key = header.get("key").longValue(-1L);
        } else {
          key = null;
        }
        final Value value = payload.body();
        new ListDownlinkRelayUpdate(this, message, index, value, key).run();
      }
    } else if ("move".equals(tag)) {
      final Value header = payload.header("move");
      final int fromIndex = header.get("from").intValue(-1);
      final int toIndex = header.get("to").intValue(-1);
      if (fromIndex > -1 && toIndex > -1) {
        final Object key;
        if (header.get("key").isDefined()) {
          key = header.get("key").longValue(-1L);
        } else {
          key = null;
        }
        new ListDownlinkRelayMove(this, message, fromIndex, toIndex, key).run();
      }
    } else if ("remove".equals(tag)) {
      final Value header = payload.header("remove");
      final int index = header.get("index").intValue(-1);
      if (index > -1) {
        final Object key;
        if (header.get("key").isDefined()) {
          key = header.get("key").longValue(-1L);
        } else {
          key = null;
        }
        new ListDownlinkRelayRemove(this, message, index, key).run();
      }
    } else if ("drop".equals(tag)) {
      final Value header = payload.header("drop");
      final int lower = header.intValue(0);
      new ListDownlinkRelayDrop(this, message, lower).run();
    } else if ("take".equals(tag)) {
      final Value header = payload.header("take");
      final int upper = header.intValue(0);
      new ListDownlinkRelayTake(this, message, upper).run();
    } else if ("clear".equals(tag)) {
      new ListDownlinkRelayClear(this, message).run();
    }
  }

  @Override
  protected void didAddDownlink(ListDownlinkView<?> view) {
    if (this.views instanceof DownlinkView) {
      isStateful(((ListDownlinkView<?>) view).isStateful());
    }
  }

  public boolean isEmpty() {
    return this.state.isEmpty();
  }

  public int size() {
    return this.state.size();
  }

  public boolean contains(Object value) {
    if (value != null) {
      return this.state.contains(value);
    } else {
      return false;
    }
  }

  public int indexOf(Object o) {
    return this.state.indexOf(o);
  }

  public int lastIndexOf(Object o) {
    return this.state.lastIndexOf(o);
  }

  public List<Value> subList(int fromIndex, int toIndex) {
    return this.state.subList(fromIndex, toIndex);
  }

  public Object[] toArray() {
    return this.state.toArray();
  }

  public <V> V[] toArray(V[] array) {
    return this.state.toArray(array);
  }

  public ListIterator<Map.Entry<Object, Value>> entryIterator() {
    return this.state.entryIterator();
  }

  public Iterator<Value> iterator() {
    return this.state.iterator();
  }

  public ListIterator<Value> listIterator() {
    return this.state.listIterator();
  }

  public ListIterator<Value> listIterator(int index) {
    return this.state.listIterator(index);
  }

  public ListIterator<Object> keyIterator() {
    return this.state.keyIterator();
  }

  public Map.Entry<Object, Value> getEntry(int index) {
    return this.state.getEntry(index);
  }

  public Map.Entry<Object, Value> getEntry(int index, Object key) {
    return this.state.getEntry(index, key);
  }

  public Value get(int index, Object key) {
    final Value value = this.state.get(index, key);
    if (value != null) {
      return value;
    }
    return Value.absent();
  }

  @SuppressWarnings("unchecked")
  public <V> boolean add(ListDownlinkView<V> view, int index, V newObject) {
    return add(view, index, newObject, null);
  }

  @SuppressWarnings("unchecked")
  public <V> boolean add(ListDownlinkView<V> view, int index, V newObject, Object key) {
    final Form<V> valueForm = view.valueForm;
    final Value newValue = valueForm.mold(newObject).toValue();
    final ListDownlinkRelayUpdate relay = new ListDownlinkRelayUpdate(this, index, newValue, key);
    relay.valueForm = (Form<Object>) valueForm;
    relay.newObject = newObject;
    relay.stage = view.stage;
    relay.run();
    if (relay.isDone() && relay.valueForm == valueForm) {
      return relay.oldObject != null;
    } else {
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  public <V> V set(ListDownlinkView<V> view, int index, V newObject) {
    return set(view, index, newObject, null);
  }

  @SuppressWarnings("unchecked")
  public <V> V set(ListDownlinkView<V> view, int index, V newObject, Object key) {
    final Form<V> valueForm = view.valueForm;
    final Value newValue = valueForm.mold(newObject).toValue();
    final ListDownlinkRelayUpdate relay = new ListDownlinkRelayUpdate(this, index, newValue, key);
    relay.valueForm = (Form<Object>) valueForm;
    relay.oldObject = newObject;
    relay.newObject = newObject;
    relay.stage = view.stage;
    relay.run();
    if (relay.isDone() && relay.valueForm == valueForm) {
      return (V) relay.oldObject;
    } else {
      return null;
    }
  }

  public <V> void move(ListDownlinkView<V> view, int fromIndex, int toIndex) {
    move(view, fromIndex, toIndex, null);
  }

  public <V> void move(ListDownlinkView<V> view, int fromIndex, int toIndex, Object key) {
    final ListDownlinkRelayMove relay = new ListDownlinkRelayMove(this, fromIndex, toIndex, key);
    relay.stage = view.stage;
    relay.run();
  }

  @SuppressWarnings("unchecked")
  public <V> V remove(ListDownlinkView<V> view, int index) {
    return remove(view, index, null);
  }

  @SuppressWarnings("unchecked")
  public <V> V remove(ListDownlinkView<V> view, int index, Object key) {
    final Form<V> valueForm = view.valueForm;
    final ListDownlinkRelayRemove relay = new ListDownlinkRelayRemove(this, index, key);
    relay.valueForm = (Form<Object>) valueForm;
    relay.stage = view.stage;
    relay.run();
    if (relay.isDone()) {
      if (relay.valueForm != valueForm && valueForm != null) {
        relay.oldObject = valueForm.cast(relay.oldValue);
        if (relay.oldObject == null) {
          relay.oldObject = valueForm.unit();
        }
      }
      return (V) relay.oldObject;
    } else {
      return null;
    }
  }

  public void drop(ListDownlinkView<?> view, int lower) {
    pushUp(Record.create(1).attr("drop", lower)); // TODO: drop to index, key
    //final ListDownlinkRelayDrop relay = new ListDownlinkRelayDrop(this, lower);
    //relay.stage = view.stage;
    //relay.run();
  }

  public void take(ListDownlinkView<?> view, int upper) {
    pushUp(Record.create(1).attr("take", upper)); // TODO: take to index, key
    //final ListDownlinkRelayTake relay = new ListDownlinkRelayTake(this, upper);
    //relay.stage = view.stage;
    //relay.run();
  }

  public void clear(ListDownlinkView<?> view) {
    final ListDownlinkRelayClear relay = new ListDownlinkRelayClear(this);
    relay.stage = view.stage;
    relay.run();
  }

  protected static final int STATEFUL = 1 << 0;
}

final class ListDownlinkRelayUpdate extends DownlinkRelay<ListDownlinkModel, ListDownlinkView<?>> {
  final EventMessage message;
  Object key;
  final int index;
  Form<Object> valueForm;
  Value oldValue;
  Value newValue;
  Object oldObject;
  Object newObject;

  ListDownlinkRelayUpdate(ListDownlinkModel model, EventMessage message, int index, Value newValue, Object key) {
    super(model, 4);
    this.message = message;
    this.index = index;
    this.key = key;
    this.newValue = newValue;
  }

  ListDownlinkRelayUpdate(ListDownlinkModel model, int index, Value newValue, Object key) {
    super(model, 1, 3);
    this.message = null;
    this.index = index;
    this.key = key;
    this.newValue = newValue;
  }

  @SuppressWarnings("unchecked")
  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      if (this.model.isStateful()) {
        Map.Entry<Object, Value> entry;
        if (this.index < this.model.state.size()) {
          entry = this.model.state.getEntry(this.index, this.key);
        } else {
          entry = null;
        }
        if (entry == null) {
          this.model.state.add(this.index, this.newValue, this.key);
          if (key == null) {
            entry = this.model.state.getEntry(this.index);
            key = entry.getKey();
          }
        } else {
          this.oldValue = entry.getValue();
          this.key = entry.getKey();
          this.model.state.set(this.index, this.newValue, this.key);
        }
      }
      if (this.oldValue == null) {
        this.oldValue = Value.absent();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  boolean runPhase(ListDownlinkView<?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.downlinkWillReceive(this.message);
      }
      return view.dispatchWillReceive(this.message.body(), preemptive);
    } else if (phase == 1) {
      this.newValue = view.downlinkWillUpdateValue(this.index, this.newValue);
      final Map.Entry<Object, Value> entry = this.model.state.getEntry(this.index, this.key);
      if (entry != null) {
        this.oldValue = entry.getValue();
        this.key = entry.getKey();
      }
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      this.valueForm = valueForm;
      this.oldObject = valueForm.cast(this.newValue);
      if (this.oldObject == null) {
        this.oldObject = valueForm.unit();
      }
      if (preemptive) {
        this.newObject = ((ListDownlinkView<Object>) view).downlinkWillUpdate(this.index, this.oldObject);
      }
      final Map.Entry<Boolean, Object> result = ((ListDownlinkView<Object>) view).dispatchWillUpdate(this.index, this.oldObject, preemptive);
      this.newObject = result.getValue();
      if (this.oldObject != this.newObject) {
        this.oldObject = this.newObject;
        this.newValue = valueForm.mold(this.newObject).toValue();
      }
      return result.getKey();
    } else if (phase == 2) {
      view.downlinkDidUpdateValue(this.index, this.newValue, this.oldValue);
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
        ((ListDownlinkView<Object>) view).downlinkDidUpdate(this.index, this.newObject, this.oldObject);
      }
      return ((ListDownlinkView<Object>) view).dispatchDidUpdate(this.index, this.newObject, this.oldObject, preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.downlinkDidReceive(this.message);
      }
      return view.dispatchDidReceive(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    if (this.message != null) {
      this.model.cueDown();
    } else {
      final Record header = Record.create(2).slot("key", Value.fromObject(this.key)).slot("index", this.index);
      this.model.pushUp(Attr.of("update", header).concat(this.newValue));
    }
  }
}

final class ListDownlinkRelayMove extends DownlinkRelay<ListDownlinkModel, ListDownlinkView<?>> {
  final EventMessage message;
  final int fromIndex;
  final int toIndex;
  Object key;
  Form<Object> valueForm;
  Value value;
  Object object;

  ListDownlinkRelayMove(ListDownlinkModel model, EventMessage message, int fromIndex, int toIndex, Object key) {
    super(model, 4);
    this.message = message;
    this.fromIndex = fromIndex;
    this.toIndex = toIndex;
    this.key = key;
  }

  ListDownlinkRelayMove(ListDownlinkModel model, int fromIndex, int toIndex, Object key) {
    super(model, 1, 3);
    this.message = null;
    this.fromIndex = fromIndex;
    this.toIndex = toIndex;
    this.key = key;
  }

  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      this.model.state.move(this.fromIndex, this.toIndex, this.key);
      if (this.value == null) {
        this.value = Value.absent();
      }
      if (this.valueForm != null) {
        this.object = this.valueForm.cast(this.value);
        if (this.object == null) {
          this.object = this.valueForm.unit();
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  boolean runPhase(ListDownlinkView<?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.downlinkWillReceive(this.message);
      }
      return view.dispatchWillReceive(this.message.body(), preemptive);
    } else if (phase == 1) {
      view.downlinkWillMoveValue(this.fromIndex, this.toIndex, this.value);
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      this.valueForm = valueForm;
      final Map.Entry<Object, Value> entry = this.model.state.getEntry(this.fromIndex, this.key);
      this.value = entry.getValue();
      this.key = entry.getKey();
      this.object = valueForm.cast(this.value); // TODO
      if (this.object == null) {
        this.object = valueForm.unit();
      }
      if (preemptive) {
        ((ListDownlinkView<Object>) view).downlinkWillMove(this.fromIndex, this.toIndex, this.value);
      }
      return ((ListDownlinkView<Object>) view).dispatchWillMove(this.fromIndex, this.toIndex, this.value, preemptive);
    } else if (phase == 2) {
      view.downlinkDidMoveValue(this.fromIndex, this.toIndex, this.value);
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      if (this.valueForm != valueForm && valueForm != null) {
        this.valueForm = valueForm;
        this.object = valueForm.cast(this.value);
        if (this.object == null) {
          this.object = valueForm.unit();
        }
      }
      if (preemptive) {
        ((ListDownlinkView<Object>) view).downlinkDidMove(this.fromIndex, this.toIndex, this.object);
      }
      return ((ListDownlinkView<Object>) view).dispatchDidMove(this.fromIndex, this.toIndex, this.object, preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.downlinkDidReceive(this.message);
      }
      return view.dispatchDidReceive(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    if (this.message != null) {
      this.model.cueDown();
    } else {
      final Record header = Record.create(3).slot("key", Value.fromObject(this.key))
                                            .slot("from", this.fromIndex).slot("to", this.toIndex);
      this.model.pushUp(Record.create(1).attr("move", header));
    }
  }
}

final class ListDownlinkRelayRemove extends DownlinkRelay<ListDownlinkModel, ListDownlinkView<?>> {
  final EventMessage message;
  final int index;
  Object key;
  Form<Object> valueForm;
  Value oldValue;
  Object oldObject;

  ListDownlinkRelayRemove(ListDownlinkModel model, EventMessage message, int index, Object key) {
    super(model, 4);
    this.message = message;
    this.index = index;
    this.key = key;
  }

  ListDownlinkRelayRemove(ListDownlinkModel model, int index, Object key) {
    super(model, 1, 3);
    this.message = null;
    this.index = index;
    this.key = key;
  }

  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      this.model.state.remove(this.index, this.key);
      if (this.oldValue == null) {
        this.oldValue = Value.absent();
      }
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
  boolean runPhase(ListDownlinkView<?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.downlinkWillReceive(this.message);
      }
      return view.dispatchWillReceive(this.message.body(), preemptive);
    } else if (phase == 1) {
      view.downlinkWillRemoveValue(this.index);
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      this.valueForm = valueForm;
      final Map.Entry<Object, Value> entry = this.model.state.getEntry(this.index, this.key);
      this.oldValue = entry.getValue();
      this.key = entry.getKey();
      this.oldObject = valueForm.cast(this.oldValue);
      if (this.oldObject == null) {
        this.oldObject = valueForm.unit();
      }
      if (preemptive) {
        view.downlinkWillRemove(this.index);
      }
      return view.dispatchWillRemove(this.index, preemptive);
    } else if (phase == 2) {
      view.downlinkDidRemoveValue(this.index, this.oldValue);
      final Form<Object> valueForm = (Form<Object>) view.valueForm;
      if (this.valueForm != valueForm && valueForm != null) {
        this.valueForm = valueForm;
        this.oldObject = valueForm.cast(this.oldValue);
        if (this.oldObject == null) {
          this.oldObject = valueForm.unit();
        }
      }
      if (preemptive) {
        ((ListDownlinkView<Object>) view).downlinkDidRemove(this.index, this.oldObject);
      }
      return ((ListDownlinkView<Object>) view).dispatchDidRemove(this.index, this.oldObject, preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.downlinkDidReceive(this.message);
      }
      return view.dispatchDidReceive(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    if (this.message != null) {
      this.model.cueDown();
    } else {
      final Record header = Record.create(2).slot("key", Value.fromObject(this.key)).slot("index", this.index);
      this.model.pushUp(Record.create(1).attr("remove", header));
    }
  }
}

final class ListDownlinkRelayDrop extends DownlinkRelay<ListDownlinkModel, ListDownlinkView<?>> {
  final EventMessage message;
  final int lower;

  ListDownlinkRelayDrop(ListDownlinkModel model, EventMessage message, int lower) {
    super(model, 4);
    this.message = message;
    this.lower = lower;
  }

  ListDownlinkRelayDrop(ListDownlinkModel model, int lower) {
    super(model, 1, 3);
    this.message = null;
    this.lower = lower;
  }

  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      if (this.model.isStateful()) {
        this.model.state.drop(this.lower);
      }
    }
  }

  @Override
  boolean runPhase(ListDownlinkView<?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.downlinkWillReceive(this.message);
      }
      return view.dispatchWillReceive(this.message.body(), preemptive);
    } else if (phase == 1) {
      if (preemptive) {
        view.downlinkWillDrop(this.lower);
      }
      return view.dispatchWillDrop(this.lower, preemptive);
    } else if (phase == 2) {
      if (preemptive) {
        view.downlinkDidDrop(this.lower);
      }
      return view.dispatchDidDrop(this.lower, preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.downlinkDidReceive(this.message);
      }
      return view.dispatchDidReceive(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    if (this.message != null) {
      this.model.cueDown();
    } else {
      this.model.pushUp(Record.create(1).attr("drop", this.lower));
    }
  }
}

final class ListDownlinkRelayTake extends DownlinkRelay<ListDownlinkModel, ListDownlinkView<?>> {
  final EventMessage message;
  final int upper;

  ListDownlinkRelayTake(ListDownlinkModel model, EventMessage message, int upper) {
    super(model, 4);
    this.message = message;
    this.upper = upper;
  }

  ListDownlinkRelayTake(ListDownlinkModel model, int upper) {
    super(model, 1, 3);
    this.message = null;
    this.upper = upper;
  }

  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      if (this.model.isStateful()) {
        this.model.state.take(this.upper);
      }
    }
  }

  @Override
  boolean runPhase(ListDownlinkView<?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.downlinkWillReceive(this.message);
      }
      return view.dispatchWillReceive(this.message.body(), preemptive);
    } else if (phase == 1) {
      if (preemptive) {
        view.downlinkWillTake(this.upper);
      }
      return view.dispatchWillTake(this.upper, preemptive);
    } else if (phase == 2) {
      if (preemptive) {
        view.downlinkDidTake(this.upper);
      }
      return view.dispatchDidTake(this.upper, preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.downlinkDidReceive(this.message);
      }
      return view.dispatchDidReceive(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    if (this.message != null) {
      this.model.cueDown();
    } else {
      this.model.pushUp(Record.create(1).attr("take", this.upper));
    }
  }
}

final class ListDownlinkRelayClear extends DownlinkRelay<ListDownlinkModel, ListDownlinkView<?>> {
  final EventMessage message;

  ListDownlinkRelayClear(ListDownlinkModel model, EventMessage message) {
    super(model, 0, 3);
    this.message = message;
  }

  ListDownlinkRelayClear(ListDownlinkModel model) {
    super(model, 3, 4);
    this.message = null;
  }

  @Override
  void beginPhase(int phase) {
    if (phase == 2) {
      if (this.model.isStateful()) {
        this.model.state.clear();
      }
    }
  }

  @Override
  boolean runPhase(ListDownlinkView<?> view, int phase, boolean preemptive) {
    if (phase == 0) {
      if (preemptive) {
        view.downlinkWillReceive(this.message);
      }
      return view.dispatchWillReceive(this.message.body(), preemptive);
    } else if (phase == 1) {
      if (preemptive) {
        view.downlinkWillClear();
      }
      return view.dispatchWillClear(preemptive);
    } else if (phase == 2) {
      if (preemptive) {
        view.downlinkDidClear();
      }
      return view.dispatchDidClear(preemptive);
    } else if (phase == 3) {
      if (preemptive) {
        view.downlinkDidReceive(this.message);
      }
      return view.dispatchDidReceive(this.message.body(), preemptive);
    } else {
      throw new AssertionError(); // unreachable
    }
  }

  @Override
  void done() {
    if (this.message != null) {
      this.model.cueDown();
    } else {
      this.model.pushUp(Record.create(1).attr("clear"));
    }
  }
}
