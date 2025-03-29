package dev.chux.gcp.crun.rest.adapters;

import java.io.IOException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.common.base.Optional;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

public class OptionalTypeAdapter<E> extends TypeAdapter<Optional<E>> {

  public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
    @Override
    public <T> TypeAdapter<T> create(
      final Gson gson,
      final TypeToken<T> type
    ) {
      final Class<T> rawType = (Class<T>) type.getRawType();
      if (rawType != Optional.class) {
        return null;
      }
      final ParameterizedType parameterizedType = (ParameterizedType) type.getType();
      final Type actualType = parameterizedType.getActualTypeArguments()[0];
      final TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(actualType));
      return new OptionalTypeAdapter(adapter);
    }
  };

  private final TypeAdapter<E> adapter;

  public OptionalTypeAdapter(
    final TypeAdapter<E> adapter
  ) {
    this.adapter = adapter;
  }

  @Override
  public void write(
    final JsonWriter out,
    final Optional<E> value
  ) throws IOException {
    if ( value.isPresent() ) {
      adapter.write(out, value.get());
    } else {
      out.nullValue();
    }
  }

  @Override
  public Optional<E> read(
    final JsonReader in
  ) throws IOException {
    final JsonToken peek = in.peek();
    if( peek != JsonToken.NULL ) {
      return fromNullable(
        adapter.read(in)
      );
    }
    in.nextNull();
    return absent();
  }

}
