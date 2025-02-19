package dev.chux.gcp.crun.model;

import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.SerializedName;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

abstract class Multivalue<T> implements Supplier<List<T>> {

  @Since(1.0)
  @Expose(deserialize=true, serialize=true)
  @SerializedName(value="values", alternate={"items"})
  private List<T> requests;

  Multivalue() {}

  protected Multivalue(final List<T> requests) {
    this.requests = Lists.newArrayList(checkNotNull(requests));
  }
  
  public List<T> values() {
    if( this.requests == null ) {
      return ImmutableList.of();
    } 
    return ImmutableList.copyOf(this.requests);
  }

  public List<T> items() {
    return this.values();
  }

  @Override
  public List<T> get() {
    return this.values();
  }

  @Override
  public String toString() {
    return toStringHelper(this)
      .addValue(this.values())
      .toString();
  }

}
