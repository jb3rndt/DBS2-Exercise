package exercise0;

import de.hpi.dbs2.ChosenImplementation;
import de.hpi.dbs2.exercise0.DummyExercise;
import org.jetbrains.annotations.NotNull;

@ChosenImplementation(false)
public class DummyExerciseJava implements DummyExercise {
    @NotNull
    @Override
    public String getYourGroupIdentifier() {
        return "V";
    }
}
