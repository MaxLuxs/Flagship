# Flagship Server

REST API server for managing feature flags and experiments.

## Features

- Full CRUD API for flags and experiments
- In-memory storage (can be replaced with database)
- Revision-based config updates
- CORS support for web clients
- JSON serialization compatible with Flagship client library

## API Endpoints

### Flags

- `GET /api/flags` - Get all flags
- `GET /api/flags/{key}` - Get specific flag
- `POST /api/flags` - Create new flag
- `PUT /api/flags/{key}` - Update flag
- `DELETE /api/flags/{key}` - Delete flag

### Experiments

- `GET /api/experiments` - Get all experiments
- `GET /api/experiments/{key}` - Get specific experiment
- `POST /api/experiments` - Create new experiment
- `PUT /api/experiments/{key}` - Update experiment
- `DELETE /api/experiments/{key}` - Delete experiment

### Config (for clients)

- `GET /config` - Get full config snapshot
- `GET /config?rev={revision}` - Get config changes since revision

## Running the Server

```bash
./gradlew :flagship-server:run
```

Server will start on `http://0.0.0.0:8080`

## Example Usage

### Create a flag

```bash
curl -X POST http://localhost:8080/api/flags \
  -H "Content-Type: application/json" \
  -d '{
    "new_feature": {
      "type": "bool",
      "value": true
    }
  }'
```

### Create an experiment

```bash
curl -X POST http://localhost:8080/api/experiments \
  -H "Content-Type: application/json" \
  -d '{
    "exp_test": {
      "variants": [
        {"name": "control", "weight": 0.5},
        {"name": "treatment", "weight": 0.5}
      ],
      "exposureType": "onAssign"
    }
  }'
```

### Get config (for Flagship client)

```bash
curl http://localhost:8080/config
```

## Architecture

- **Storage**: In-memory storage with thread-safe Mutex
- **Revision**: UUID-based revision tracking for efficient updates
- **Models**: Uses Flagship core models and REST provider models

## Future Enhancements

- Database persistence (PostgreSQL, MongoDB)
- Authentication and authorization
- Admin dashboard
- Analytics and metrics
- Webhooks for config changes
- Multi-environment support

