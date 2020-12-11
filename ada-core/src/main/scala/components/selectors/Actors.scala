package ada.core.components.selectors

import scala.collection.mutable.{Map => MutableMap}

import ada._
import ada.core.interface._
import ada.core.components.distributions._

sealed trait Actor

trait CombinedActor[ModelID, ModelData, ModelAction]
    extends SimpleActor[ModelID, ModelData, ModelAction]
    with ContextualActor[ModelID, ModelData, ModelAction]
    with StackableActor[ModelID, ModelData, ModelAction]
    with StackableActor2[ModelID, ModelData, ModelAction]

trait SimpleActor[ModelID, ModelData, ModelAction]
    extends Selector[ModelID, ModelData, ModelAction]
    with Actor{

    def _actImpl[AggregateReward <: SimpleDistribution]
                (models: ModelID => Model[ModelData, ModelAction],
                modelKeys: () => List[ModelID],
                modelRewards: ModelID => AggregateReward,
                epsilon: Double,
                data: ModelData): (ModelAction, ModelID) 
}

trait ContextualActor[ModelID, ModelData, ModelAction]
    extends Selector[ModelID, ModelData, ModelAction]
    with Actor{
    def _actImpl[Context, AggregateReward <: ContextualDistribution[Context]]
    			(models: ModelID => Model[ModelData, ModelAction],
                modelKeys: () => List[ModelID],
                modelRewards: ModelID => AggregateReward,
                epsilon: Double,
                data: ModelData,
                context: Context): (ModelAction, ModelID) 
}

trait StackableActor[ModelID, ModelData, ModelAction]
    extends Selector[ModelID, ModelData, ModelAction]
    with Actor{
    def _actImpl[AggregateReward <: SimpleDistribution]
                (models: ModelID => StackableModel[ModelID, ModelData, ModelAction],
                modelKeys: () => List[ModelID],
                modelRewards: ModelID => AggregateReward,
                epsilon: Double,
                data: ModelData,
                selectedIds: List[ModelID]): (ModelAction, List[ModelID]) 
}

trait StackableActor2[ModelID, ModelData, ModelAction]
    extends Selector[ModelID, ModelData, ModelAction]
    with Actor{
    def _actImpl2[AggregateReward <: ContextualDistribution[ModelData]]
                (models: ModelID => StackableModel2[ModelID, ModelData, ModelAction],
                modelKeys: () => List[ModelID],
                modelRewards: ModelID => AggregateReward,
                epsilon: Double,
                data: ModelData,
                selectedIds: List[ModelID]): (ModelAction, List[ModelID])
}

trait AbstractGreedy[ModelID, ModelData, ModelAction]
    extends Selector[ModelID, ModelData, ModelAction]
    with Actor{

    def _actImpl[AggregateReward <: SimpleDistribution]
                (models: ModelID => Model[ModelData, ModelAction],
                modelKeys: () => List[ModelID],
                modelRewards: ModelID => AggregateReward,
                epsilon: Double,
                data: ModelData): (ModelAction, ModelID) = {

        val modelsSorted = _sortModel[AggregateReward](models, modelKeys, modelRewards)

        if(epsilon == 0.0 || rnd.nextDouble() > epsilon) {
            val selectedModelId = modelsSorted.head._1
            val selectedModel = models(selectedModelId)
            (selectedModel.act(data), selectedModelId)
        }
        else _selectModel(models, modelKeys, modelsSorted.tail, data)    }

    def _actImpl[Context, AggregateReward <: ContextualDistribution[Context]]
    			(models: ModelID => Model[ModelData, ModelAction],
                modelKeys: () => List[ModelID],
                modelRewards: ModelID => AggregateReward,
                epsilon: Double,
                data: ModelData,
                context: Context): (ModelAction, ModelID) = {

        val modelsSorted = _sortModel[Context, AggregateReward](models, modelKeys, modelRewards, context)

        if(epsilon == 0.0 || rnd.nextDouble() > epsilon) {
            val selectedModelId = modelsSorted.head._1
            val selectedModel = models(selectedModelId)
            (selectedModel.act(data), selectedModelId)
        }
        else _selectModel(models, modelKeys, modelsSorted.tail, data)    }




    def _actImpl[AggregateReward <: SimpleDistribution]
                (models: ModelID => StackableModel[ModelID, ModelData, ModelAction],
                modelKeys: () => List[ModelID],
                modelRewards: ModelID => AggregateReward,
                epsilon: Double,
                data: ModelData,
                selectedIds: List[ModelID]): (ModelAction, List[ModelID]) = {

        val modelsSorted = _sortModel[AggregateReward](models, modelKeys, modelRewards)

        if(epsilon == 0.0 || rnd.nextDouble() > epsilon) {
            val selectedModelId = modelsSorted.head._1
            val selectedModel = models(selectedModelId)
            selectedModel.actWithID(data, selectedIds ::: List(selectedModelId))
        }
        else {
            val (action, modelId) = _selectModel(models, modelKeys, modelsSorted.tail, data)
            (action, selectedIds ::: List(modelId))
        }
    }

    def _actImpl2[AggregateReward <: ContextualDistribution[ModelData]]
                (models: ModelID => StackableModel2[ModelID, ModelData, ModelAction],
                modelKeys: () => List[ModelID],
                modelRewards: ModelID => AggregateReward,
                epsilon: Double,
                data: ModelData,
                selectedIds: List[ModelID]): (ModelAction, List[ModelID]) = {
                                    //ModelData is also used as Context!!!
        val modelsSorted = _sortModel[ModelData, AggregateReward](models, modelKeys, modelRewards, data)

        if(epsilon == 0.0 || rnd.nextDouble() > epsilon) {
            val selectedModelId = modelsSorted.head._1
            val selectedModel = models(selectedModelId)
            selectedModel.actWithID(data, selectedIds ::: List(selectedModelId))
        }
        else {
            val (action, modelId) = _selectModel(models, modelKeys, modelsSorted.tail, data)
            (action, selectedIds ::: List(modelId))
        }
    }
}


//not used so far
trait Softmax[ModelID, ModelData, ModelAction]
    extends SoftmaxSelector[ModelID, ModelData, ModelAction]{
    def _actImpl[AggregateReward <: SimpleDistribution](models: ModelID => Model[ModelData, ModelAction],
                modelKeys: () => List[ModelID],
                 modelRewards: ModelID => AggregateReward,
                 data: ModelData): (ModelAction, ModelID) = {
        val modelsSorted = _sortModel[AggregateReward](models, modelKeys, modelRewards)
        _selectModel(models, modelKeys, modelsSorted, data)
    }
    def _actImpl[Context, AggregateReward <: ContextualDistribution[Context]](models: ModelID => Model[ModelData, ModelAction],
                 modelKeys: () => List[ModelID],
                 modelRewards: ModelID => AggregateReward,
                 context: Context,
                 data: ModelData): (ModelAction, ModelID) = {
        val modelsSorted = _sortModel[Context, AggregateReward](models, modelKeys, modelRewards, context)
        _selectModel(models, modelKeys, modelsSorted, data)
    }
}
