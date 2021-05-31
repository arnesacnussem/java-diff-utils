package com.github.difflib.examples;

import com.github.difflib.DiffUtils;
import com.github.difflib.TestConstants;
import com.github.difflib.patch.*;
import com.google.gson.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import static com.github.difflib.GenerateUnifiedDiffTest.fileToLines;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GsonSerializeTest {
    List<String> origLines = fileToLines(TestConstants.MOCK_FOLDER + "original.txt");
    List<String> revLines = fileToLines(TestConstants.MOCK_FOLDER + "revised.txt");

    public GsonSerializeTest() throws IOException {
    }

    @Test
    public void serialize() {
        Patch<String> diff = DiffUtils.diff(origLines, revLines);
        String json = new Gson().toJson(diff);
        System.out.println(json);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void deserialize() throws PatchFailedException {
        Patch<String> diff = DiffUtils.diff(origLines, revLines);
        String json = new Gson().toJson(diff);
        Patch<String> patch = new GsonBuilder()
                .registerTypeAdapter(Chunk.class, new ChunkDeserializer())
                .registerTypeAdapter(AbstractDelta.class, new AbstractDeltaDeserializer())
                .registerTypeAdapter(ConflictOutput.class, (JsonDeserializer<ConflictOutput<String>>) (_ignored1, _ignored2, _ignored3) -> null)
                .create()
                .fromJson(json, Patch.class);
        assertThat(patch.applyTo(origLines)).isEqualTo(revLines);
    }

    public static class AbstractDeltaDeserializer implements JsonDeserializer<AbstractDelta<String>> {

        @Override
        public AbstractDelta<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            DeltaType type = DeltaType.valueOf(object.get("type").getAsString());
            Chunk<String> source = context.deserialize(object.get("source"), Chunk.class);
            Chunk<String> target = context.deserialize(object.get("target"), Chunk.class);
            switch (type) {
                case EQUAL:
                    return new EqualDelta<>(source, target);
                case INSERT:
                    return new InsertDelta<>(source, target);
                case DELETE:
                    return new DeleteDelta<>(source, target);
                case CHANGE:
                    return new ChangeDelta<>(source, target);
            }
            return null;
        }
    }

    public static class ChunkDeserializer implements JsonDeserializer<Chunk<String>> {

        @Override
        public Chunk<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            int position = object.get("position").getAsInt();
            List<String> lines = context.deserialize(object.get("lines"), List.class);
            List<Integer> changePosition = context.deserialize(object.get("changePosition"), List.class);
            return new Chunk<>(position, lines, changePosition);
        }
    }
}
