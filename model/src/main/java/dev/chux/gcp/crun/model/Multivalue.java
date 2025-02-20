package dev.chux.gcp.crun.model;

import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.SerializedName;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.SerializedName;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Multivalue<T> implements Supplier<List<T>> {

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(
    value="values",
    alternate={
      "items",
      "tasks",
      "elements",
      "commands",
      "jobs"
    }
  )
  private List<T> values;

  Multivalue() {}

  protected Multivalue(final List<T> values) {
    this.values = Lists.newArrayList(checkNotNull(values));
  }
  
  public final List<T> values() {
    if (this.values == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(this.values);
  }

  @Override
  public List<T> get() {
    return this.values();
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .addValue(this.get())
      .toString();
  }

}
