package ada

import org.scalacheck._
import Arbitrary._
import Gen._
import Prop._
import io.circe.Json


import ada.core.models.{StaticModel, GenericStaticModel}
import ada.core.ensembles.GreedySoftmaxEnsemble
import ada.generators.{ConstantGenerator}
import ada.generators.Generator
import ada.core.interface.{AdaEnsemble}
import ada.core.components.distributions.ExpDouble
import _root_.breeze.stats.mode


class TestGreedySoftmax extends Properties("TestSpecificEEGreedySoftmax") {

    def isclose(n1: Double, n2: Double): Boolean = {
        math.abs(n1 - n2) <= (math.max(math.abs(n1), math.abs(n2)) * 0.1) + 0.05
    }

    //def evaluationFn[ActionType](action: ActionType, correctAction: ActionType): Double =  if(action == correctAction) 1.0 else 0.1



    def makeGenerator[ModelId, ModelData, ModelAction](idGenerator: Gen[ModelId], dataGenerator: Gen[ModelData], actionGenerator: Gen[ModelAction]) = {
        val generator = for{
            modelData <- dataGenerator
            const1 <- actionGenerator
            const2 <- actionGenerator suchThat(_ != const1)
            id1 <- idGenerator
            id2 <- idGenerator suchThat(_ != id1)
            id3 <- idGenerator suchThat(id => id != id1 && id != id2)
            etaSource <- Gen.choose(10, 1000)
        } yield {
            val eta = etaSource.toDouble/1500.0


            val generator = new ConstantGenerator(const1)

            val models = List(new GenericStaticModel[ModelId, ModelData, ModelAction, ExpDouble](const1)(x => Json.fromString(x.toString())),
                          new GenericStaticModel[ModelId, ModelData, ModelAction, ExpDouble](const2)(x => Json.fromString(x.toString())),
                          new GenericStaticModel[ModelId, ModelData, ModelAction, ExpDouble](const2)(x => Json.fromString(x.toString())))

            val modelMap = Map(id1 -> models(0), id2 -> models(1), id3 -> models(2))
            val ensemble = new GreedySoftmaxEnsemble[ModelId, ModelData, ModelAction, ExpDouble](
                                                                    (id) => modelMap(id),
                                                                    () => modelMap.keys.toList,
                                                                    Map(id1 -> new ExpDouble(1.0), id2 -> new ExpDouble(1.0), id3 -> new ExpDouble(3.0)),
                                                                    eta)
            
            (eta, (id1, id2, id3), (const1, const2), (generator, models, ensemble), modelData)
        }
        generator
    }


    private def report(eta: Double, rounds: List[(Any, Any)], tests:List[Boolean], rewardsMap: List[(Any, Any)]): Unit = {
        println("-----")
        println(rewardsMap.toMap)
        println(f"eta: $eta")
        println(rounds.map(_._2).groupBy(identity).mapValues(_.size).mapValues(1000*_/rounds.length).toMap)
        tests.map(println)
    }
    
    
    def testTypedEEGreedySoftmax[ModelId, ModelData, ModelAction](name: String, nActions: Int, idGenerator: Gen[ModelId], dataGenerator: Gen[ModelData], actionGenerator: Gen[ModelAction]) = {
        val generator = makeGenerator(arbitrary[String], arbitrary[Double], arbitrary[Double])

        property(name + " - proportions of model selections correspond to eta value - initial reward") = forAll(generator){
            tuple => {
                val (eta, (id1, id2, id3), (const1, const2), (generator, models, ensemble), modelData) = tuple
                val rounds = for {
                    i <- (0 until nActions)
                } yield{
                    val (action, selectedIds) = ensemble.actWithID(i, List())
                    (action, selectedIds(0))
                }

                val test1 = isclose(rounds.count(_._2 == id3).toDouble, nActions*(1-eta))

                val test2 = isclose(rounds.count(t => t._2 == id1).toDouble/rounds.length, rounds.count(t => t._2 == id2).toDouble/rounds.length)
                val result = test1 && test2 

                if(result == false) report(eta, rounds.toList, List(test1, test2), ensemble.modelRewards.toList)
                result
            }
        }
        property(name + " - proportions of model selections correspond to eta value - after learning") = forAll(generator){
            tuple => {
                val (eta, (id1, id2, id3), (const1, const2), (generator, models, ensemble), modelData) = tuple

                (0 until 100).map{ i =>
                    val (action, selectedIds) = ensemble.actWithID(modelData, List())
                    //ensemble.update(selectedIds(0), if(action == const1) 3.0 else 0.0)
                    require(selectedIds.length == 1)
                    val reward: Reward = new Reward(if(action == const1) 3.0 else 1.0)
                    /*println(action)
                    println(const1)
                    println(reward)*/
                    ensemble.update(selectedIds, modelData, reward:Reward)
                }

                val rounds = for {
                    i <- (0 until nActions)
                } yield{
                    val (action, selectedIds) = ensemble.actWithID(i, List())
                    (action, selectedIds(0))
                }
                val test1 = isclose(rounds.count(_._2 == id1).toDouble, (1-eta)*nActions)
                val test2 = isclose(rounds.count(t => t._2 == id2).toDouble/rounds.length, rounds.count(t => t._2 == id3).toDouble/rounds.length)
                val result = test1 && test2

                if (result == false) report(eta, rounds.toList, List(test1, test2), ensemble.modelRewards.toList)
                result
                
            }
        }
    }

    
    //these are the actual test executions
    testTypedEEGreedySoftmax("SDD", 1000, arbitrary[String], arbitrary[Double], arbitrary[Double])
    testTypedEEGreedySoftmax("SII", 1000, arbitrary[String], arbitrary[Int], arbitrary[Int])
    testTypedEEGreedySoftmax("IDIX", 1000, arbitrary[Int], arbitrary[Double], Gen.pick(5, (0 until 1000).toSet))

}