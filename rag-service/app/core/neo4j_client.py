import logging
from neo4j import Driver, GraphDatabase

from app.core.config import Settings
from app.core.neo4j_env import resolve_neo4j_password

logger = logging.getLogger(__name__)

_driver: Driver | None = None
_driver_fingerprint: tuple[str, str, str, str | None] | None = None


def get_neo4j_driver() -> Driver | None:
    """
    每次用 Settings() 读 .env，避免进程启动时单例 settings 缓存旧值导致改 .env 后不生效。
    连接参数变化时会关闭旧 Driver 再建连。
    """
    global _driver, _driver_fingerprint
    cfg = Settings()
    if not cfg.neo4j_enabled:
        return None
    password = resolve_neo4j_password() or (cfg.neo4j_password or "").strip()
    if not password:
        logger.warning("NEO4J_ENABLED=true 但未配置 NEO4J_PASSWORD，Neo4j 驱动未初始化")
        return None
    fp = (cfg.neo4j_uri, cfg.neo4j_user, password, cfg.neo4j_database)
    if _driver is not None and _driver_fingerprint != fp:
        close_neo4j_driver()
    if _driver is None:
        _driver = GraphDatabase.driver(
            cfg.neo4j_uri,
            auth=(cfg.neo4j_user, password),
        )
        _driver_fingerprint = fp
    return _driver


def close_neo4j_driver() -> None:
    global _driver, _driver_fingerprint
    if _driver is not None:
        _driver.close()
        _driver = None
    _driver_fingerprint = None


def neo4j_session_config() -> dict:
    cfg = Settings()
    out: dict = {}
    if cfg.neo4j_database:
        out["database"] = cfg.neo4j_database
    return out
