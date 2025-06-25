FROM python:3.11-slim

# Install required packages
RUN pip install flask flask-cors paho-mqtt

# Create app directory
WORKDIR /app

# Copy application files
COPY health_data_api.py /app/
COPY requirements.txt /app/ 2>/dev/null || echo "flask==3.0.0" > /app/requirements.txt && echo "flask-cors==4.0.0" >> /app/requirements.txt && echo "paho-mqtt==1.6.1" >> /app/requirements.txt

# Install dependencies
RUN pip install -r requirements.txt

# Expose port
EXPOSE 5001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:5001/health || exit 1

# Run the application
CMD ["python", "health_data_api.py"]