FROM python:3.9-slim-bullseye

# Install system dependencies
RUN apt-get update && apt-get install -y \
    bluez \
    bluetooth \
    libbluetooth-dev \
    libglib2.0-dev \
    libboost-python-dev \
    libboost-thread-dev \
    mosquitto-clients \
    bc \
    sudo \
    build-essential \
    make \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy requirements first for better caching
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy application files
COPY . .

# Create user directory for config and data
RUN mkdir -p /app/user

# Make scripts executable
RUN chmod +x import_data_mqtt.sh

# Create a non-root user for BLE access
RUN useradd -m -s /bin/bash healthdata && \
    usermod -aG bluetooth healthdata && \
    echo "healthdata ALL=(ALL) NOPASSWD: /usr/bin/hciconfig, /usr/sbin/hciconfig, /app/user/mac_spoof" >> /etc/sudoers

# Volume for configuration and data persistence
VOLUME ["/app/user"]

# Environment variables
ENV PYTHONUNBUFFERED=1

# Copy entrypoint script
COPY docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

# Default to running as healthdata user
USER healthdata

# Entrypoint
ENTRYPOINT ["/app/docker-entrypoint.sh"]

# Default command
CMD ["./import_data_mqtt.sh", "-l"]