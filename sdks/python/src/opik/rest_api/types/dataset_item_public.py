# This file was auto-generated by Fern from our API Definition.

from ..core.pydantic_utilities import UniversalBaseModel
import typing
from .dataset_item_public_source import DatasetItemPublicSource
from .json_node import JsonNode
from .experiment_item_public import ExperimentItemPublic
import datetime as dt
from ..core.pydantic_utilities import IS_PYDANTIC_V2
import pydantic


class DatasetItemPublic(UniversalBaseModel):
    id: typing.Optional[str] = None
    trace_id: typing.Optional[str] = None
    span_id: typing.Optional[str] = None
    source: DatasetItemPublicSource
    data: JsonNode
    experiment_items: typing.Optional[typing.List[ExperimentItemPublic]] = None
    created_at: typing.Optional[dt.datetime] = None
    last_updated_at: typing.Optional[dt.datetime] = None
    created_by: typing.Optional[str] = None
    last_updated_by: typing.Optional[str] = None

    if IS_PYDANTIC_V2:
        model_config: typing.ClassVar[pydantic.ConfigDict] = pydantic.ConfigDict(
            extra="allow", frozen=True
        )  # type: ignore # Pydantic v2
    else:

        class Config:
            frozen = True
            smart_union = True
            extra = pydantic.Extra.allow
