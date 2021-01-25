package ada.core.ensembles

import breeze.stats.mode
import io.circe.Json

import ada._
import ada.core.interface._
import ada.core.components.selectors._
import ada.core.components.distributions._


abstract class ContextualGreedyAbstract
	[ModelID, Context, ModelData, ModelAction,
	 AggregateReward <: ContextualDistribution[Context]]
    (models: ModelID  => ContextualModel[ModelID, Context, ModelData, ModelAction],
     modelKeys: () => List[ModelID],
    modelRewards: Map[ModelID, AggregateReward],
    epsilon: Double)
    extends ContextualEnsemble[ModelID, Context, ModelData, ModelAction, AggregateReward](models, modelKeys, modelRewards)
    with ContextualActor[ModelID, Context, ModelData, ModelAction]{

    def actWithID(context: Context, data: ModelData, modelIds: List[ModelID]): (ModelAction, List[ModelID]) = {
        _actImpl[Context, AggregateReward](models, modelKeys, modelRewards, epsilon, data, context, modelIds)
    }

}

class ContextualGreedy
	[ModelID, Context, ModelData, ModelAction,
	 AggregateReward <: ContextualDistribution[Context]]
    (models: ModelID  => ContextualModel[ModelID, Context, ModelData, ModelAction],
     modelKeys: () => List[ModelID],
    modelRewards: Map[ModelID, AggregateReward],
    epsilon: Double)
    extends ContextualGreedyAbstract[ModelID, Context, ModelData, ModelAction, AggregateReward](models, modelKeys, modelRewards, epsilon)
    with GreedyRandom[ModelID, ModelData, ModelAction]
