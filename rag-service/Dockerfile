FROM python:3.12-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
RUN useradd -r -u 10001 -m appuser
COPY requirements.txt .
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple -r requirements.txt
COPY --chown=appuser:appuser app ./app
COPY --chown=appuser:appuser knowledge ./knowledge
COPY --chown=appuser:appuser data ./data
COPY --chown=appuser:appuser scripts ./scripts
USER appuser
EXPOSE 9001
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "9001"]
