import { SpringPage } from './auth.model';

export interface AuditLog {
  id: string;
  eventType: string;
  action: string;
  source: string;
  actorId?: string | null;
  actorExternalId?: string | null;
  actorName?: string | null;
  actorRole?: string | null;
  entityType?: string | null;
  entityId?: string | null;
  description?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  traceId?: string | null;
  metadata?: Record<string, unknown>;
  createdAt: string;
}

export interface AuditLogFilter {
  eventType?: string;
  source?: string;
  actorName?: string;
  entityType?: string;
  entityId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export type AuditLogPage = SpringPage<AuditLog>;
