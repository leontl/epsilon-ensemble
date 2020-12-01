package ada.core.components.learners

import scala.collection.mutable.{Map => MutableMap}

import ada._
import ada.core.interface._
import ada.core.components.distributions._


trait SelectModel[ModelID, ModelData, ModelAction]{
    protected val rnd = new scala.util.Random(101)

    def _sortModel[AggregateReward <: SimpleDistribution](models: ModelID => Model[ModelData, ModelAction],
                 modelKeys: () => List[ModelID],
                 modelRewards: ModelID => AggregateReward): List[(ModelID, Double)] = {
        val modelIds = modelKeys()
        val modelsSorted = modelIds.map(modelId => (modelId, modelRewards(modelId).draw))
                                        .toList
                                        .sortWith(_._2 > _._2)
        modelsSorted
    }

    def _sortModel[Context, AggregateReward <: ContextualDistribution[Context]]
    			  (models: ModelID => Model[ModelData, ModelAction],
                   modelKeys: () => List[ModelID],
                   modelRewards: ModelID => AggregateReward,
                   context: Context): List[(ModelID, Double)] = {
        val modelIds = modelKeys()
        val modelsSorted = modelIds.map(modelId => (modelId, modelRewards(modelId).draw(context)))
                                        .toList
                                        .sortWith(_._2 > _._2)
        modelsSorted
    }

    def _selectModel(models: ModelID => Model[ModelData, ModelAction],
                   modelKeys: () => List[ModelID],
            aggregateRewardsDouble: List[(ModelID, Double)],
            data: ModelData): (ModelAction, ModelID)
}


trait SelectWithSoftmax[ModelID, ModelData, ModelAction]
    extends SelectModel[ModelID, ModelData, ModelAction]{

    def _selectModel(models: ModelID => Model[ModelData, ModelAction],
                     modelKeys: () => List[ModelID],
                     aggregateRewardsDouble: List[(ModelID, Double)],
                     data: ModelData): (ModelAction, ModelID) = {
        val totalReward: Double = aggregateRewardsDouble.foldLeft(0.0)((agg, tup) => agg + tup._2)
        val cumulativeProb: List[(Probability, Probability)] = 
        	aggregateRewardsDouble
        		.scanLeft((0.0, 0.0))((acc, item) => (acc._2, acc._2 + item._2/totalReward)).tail

        val modelsCumulativeProb: List[(ModelID, (Probability, Probability))] = 
        	aggregateRewardsDouble.map(_._1).zip(cumulativeProb)

        val selector = rnd.nextDouble()
        val selectedModelId: ModelID = 
        	modelsCumulativeProb.filter{case(model, bounds) => 
        								(selector >= bounds._1) && (selector <= bounds._2)}(0)._1

        val selectedModel: Model[ModelData, ModelAction] = models(selectedModelId)
        (selectedModel.act(data), selectedModelId)
    }
}