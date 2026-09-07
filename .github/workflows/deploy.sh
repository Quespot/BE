#!/usr/bin/env bash

set -Eeuo pipefail

readonly DEPLOY_DIR="/opt/quespot"
readonly COMPOSE_FILE="${DEPLOY_DIR}/compose.yaml"
readonly ENV_FILE="${DEPLOY_DIR}/.env"
readonly IMAGE_ENV_FILE="${DEPLOY_DIR}/.image.env"
readonly LAST_IMAGE_FILE="${DEPLOY_DIR}/.last-successful-image"
readonly HEALTH_URL="http://127.0.0.1/actuator/health"
readonly HEALTH_RETRIES=18
readonly HEALTH_INTERVAL_SECONDS=5

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <ecr-image-uri>" >&2
    exit 2
fi

readonly NEW_IMAGE="$1"
readonly REGISTRY_HOST="${NEW_IMAGE%%/*}"

exec 9>/var/lock/quespot-deploy.lock
if ! flock -n 9; then
    echo "Another Quespot deployment is already running." >&2
    exit 1
fi

cd "${DEPLOY_DIR}"

if [[ ! -f "${ENV_FILE}" ]]; then
    echo "${ENV_FILE} does not exist." >&2
    exit 1
fi

if [[ ! -f "${COMPOSE_FILE}" ]]; then
    echo "${COMPOSE_FILE} does not exist." >&2
    exit 1
fi

env_mode="$(stat -c '%a' "${ENV_FILE}")"
if [[ "${env_mode}" != "600" ]]; then
    echo "${ENV_FILE} permissions must be 600 (current: ${env_mode})." >&2
    exit 1
fi

required_variables=(
    DB_URL
    DB_USERNAME
    DB_PASSWORD
    REDIS_PASSWORD
    JWT_SECRET
    JWT_REFRESH_TOKEN_COOKIE_SECURE
    CORS_ALLOWED_ORIGINS
    MAIL_USERNAME
    MAIL_PASSWORD
    MAIL_VERIFICATION_CODE_SECRET
    SWAGGER_ENABLED
)

missing_variables=()
for variable in "${required_variables[@]}"; do
    if ! grep --quiet --extended-regexp "^${variable}=.+" "${ENV_FILE}"; then
        missing_variables+=("${variable}")
    fi
done

if (( ${#missing_variables[@]} > 0 )); then
    printf 'Required values are missing from %s: %s\n' \
        "${ENV_FILE}" "${missing_variables[*]}" >&2
    exit 1
fi

previous_image=""
if [[ -f "${LAST_IMAGE_FILE}" ]]; then
    previous_image="$(<"${LAST_IMAGE_FILE}")"
fi

login_to_ecr() {
    aws ecr get-login-password --region ap-northeast-2 \
        | docker login --username AWS --password-stdin "${REGISTRY_HOST}"
}

write_image_env() {
    local image="$1"
    printf 'APP_IMAGE=%s\n' "${image}" > "${IMAGE_ENV_FILE}"
    chmod 600 "${IMAGE_ENV_FILE}"
}

run_compose() {
    docker compose \
        --env-file "${ENV_FILE}" \
        --env-file "${IMAGE_ENV_FILE}" \
        -f "${COMPOSE_FILE}" \
        "$@"
}

start_image() {
    local image="$1"
    write_image_env "${image}" || return 1
    run_compose pull app || return 1
    run_compose up -d redis || return 1
    run_compose up -d --no-deps --force-recreate app nginx certbot || return 1
}

wait_for_health() {
    local attempt
    for ((attempt = 1; attempt <= HEALTH_RETRIES; attempt++)); do
        if curl --fail --silent --show-error \
            --connect-timeout 3 \
            --max-time 5 \
            "${HEALTH_URL}" \
            | grep --quiet '"status":"UP"'; then
            return 0
        fi
        sleep "${HEALTH_INTERVAL_SECONDS}"
    done
    return 1
}

rollback() {
    if [[ -z "${previous_image}" ]]; then
        echo "No previous image is available for rollback." >&2
        return 1
    fi

    echo "Rolling back to ${previous_image}."
    start_image "${previous_image}" || return 1
    wait_for_health || return 1
}

login_to_ecr
echo "Deploying ${NEW_IMAGE}."
if ! start_image "${NEW_IMAGE}"; then
    echo "Failed to start ${NEW_IMAGE}." >&2
    run_compose logs --tail 100 app >&2 || true

    if rollback; then
        echo "Rollback succeeded: ${previous_image}" >&2
    else
        echo "Rollback failed or was unavailable." >&2
    fi

    exit 1
fi

if wait_for_health; then
    printf '%s\n' "${NEW_IMAGE}" > "${LAST_IMAGE_FILE}"
    chmod 600 "${LAST_IMAGE_FILE}"
    echo "Deployment succeeded: ${NEW_IMAGE}"
    exit 0
fi

echo "Health check failed for ${NEW_IMAGE}." >&2
run_compose logs --tail 100 app >&2 || true

if rollback; then
    echo "Rollback succeeded: ${previous_image}" >&2
else
    echo "Rollback failed or was unavailable." >&2
fi

exit 1
