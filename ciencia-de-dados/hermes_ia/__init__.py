"""Hermes IA — pipeline de análise de transcrições com modelos do Hugging Face."""
from .config import ConfigPipeline, config_perfil
from .pipeline import VERSAO, HermesPipeline

__all__ = ["HermesPipeline", "ConfigPipeline", "config_perfil", "VERSAO"]
