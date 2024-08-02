import com.apollographql.cli.main
import org.junit.Test
import kotlin.test.Ignore

class MainTest {
  @Ignore
  @Test
  fun test() {
    main(arrayOf("download-schema", "--endpoint", "https://apollo-fullstack-tutorial.herokuapp.com/graphql", "--schema", "schema.graphqls"))
  }
}