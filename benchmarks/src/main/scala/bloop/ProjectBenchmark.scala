package bloop

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

import bloop.cli.CommonOptions
import bloop.data.ClientInfo
import bloop.engine.ExecutionContext
import bloop.engine.NoPool
import bloop.io.AbsolutePath
import bloop.logging.NoopLogger

import monix.execution.misc.NonFatal
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Mode.SampleTime
import org.openjdk.jmh.annotations._

@State(Scope.Benchmark)
@BenchmarkMode(Array(SampleTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
class ProjectBenchmark {
  final def loadProject(configDir: AbsolutePath): Unit = {
    import bloop.engine.State
    val client = ClientInfo.CliClientInfo(useStableCliDirs = true, () => true)
    val t = State.loadActiveStateFor(configDir, client, NoPool, CommonOptions.default, NoopLogger)
    val duration = FiniteDuration(10, TimeUnit.SECONDS)
    val handle = t.runAsync(ExecutionContext.scheduler)
    try Await.result(handle, duration)
    catch {
      case NonFatal(t) => handle.cancel(); throw t
      case i: InterruptedException => handle.cancel(); throw i
    }
    ()
  }

  private var sbt: AbsolutePath = _
  private var lichess: AbsolutePath = _
  private var akka: AbsolutePath = _

  @Setup(Level.Trial) def spawn(): Unit = {
    def existing(path: AbsolutePath): AbsolutePath = {
      assert(Files.exists(path.underlying))
      path
    }

    sbt = existing(AbsolutePath(CommunityBuild.getConfigDirForBenchmark("sbt")))
    lichess = existing(AbsolutePath(CommunityBuild.getConfigDirForBenchmark("lichess")))
    akka = existing(AbsolutePath(CommunityBuild.getConfigDirForBenchmark("akka")))
  }

  @Benchmark
  def loadSbtProject(): Unit = {
    loadProject(sbt)
  }

  @Benchmark
  def loadLichessProject(): Unit = {
    loadProject(lichess)
  }

  @Benchmark
  def loadAkkaProject(): Unit = {
    loadProject(akka)
  }
}
