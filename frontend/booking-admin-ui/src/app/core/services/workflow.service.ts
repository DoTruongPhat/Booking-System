import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface StartProcessRequest {
  processKey: string;
  businessKey: string;
  variables: Record<string, any> | null;
}

export interface ProcessInstanceResponse {
  processInstanceId: string;
  processDefinitionId: string;
  businessKey: string;
  ended: boolean;
}

export interface TaskResponse {
  taskId: string;
  taskName: string;
  taskDefinitionKey: string;
  processInstanceId: string;
  businessKey: string;
  assignee: string | null;
  created: string;
  variables: Record<string, any>;
}

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  private readonly baseUrl = '/api/workflows';

  constructor(private http: HttpClient) {}

  startProcess(request: StartProcessRequest): Observable<ProcessInstanceResponse> {
    return this.http
      .post<any>(`${this.baseUrl}/hotel-approvals`, this.toHotelApprovalRequest(request), {
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  getProcess(processInstanceId: string): Observable<ProcessInstanceResponse> {
    return this.http
      .get<any>(`${this.baseUrl}/process/${processInstanceId}`, { withCredentials: true })
      .pipe(map((res) => res.data));
  }

  getProcessByBusinessKey(businessKey: string): Observable<ProcessInstanceResponse> {
    return this.http
      .get<any>(`${this.baseUrl}/process`, {
        params: new HttpParams().set('businessKey', businessKey),
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  getTasks(
    params: {
      candidateGroup?: string;
      assignee?: string;
    } = {},
  ): Observable<TaskResponse[]> {
    let httpParams = new HttpParams();
    if (params.candidateGroup) httpParams = httpParams.set('candidateGroup', params.candidateGroup);
    if (params.assignee) httpParams = httpParams.set('assignee', params.assignee);

    return this.http
      .get<any>(`${this.baseUrl}/tasks`, {
        params: httpParams,
        withCredentials: true,
      })
      .pipe(map((res) => res.data));
  }

  getTask(taskId: string): Observable<TaskResponse> {
    return this.http
      .get<any>(`${this.baseUrl}/tasks/${taskId}`, { withCredentials: true })
      .pipe(map((res) => res.data));
  }

  claimTask(taskId: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/tasks/${taskId}/claim`, {}, { withCredentials: true });
  }

  unclaimTask(taskId: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/tasks/${taskId}/unclaim`, {}, { withCredentials: true });
  }

  completeTask(taskId: string, variables: Record<string, any>): Observable<any> {
    return this.decideHotelApproval(
      taskId,
      variables['decision'],
      variables['comment'],
    );
  }

  decideHotelApproval(
    taskId: string,
    decision: 'APPROVED' | 'REJECTED',
    comment?: string,
  ): Observable<any> {
    return this.http.post<any>(
      `${this.baseUrl}/hotel-approvals/tasks/${taskId}/decision`,
      { decision, comment },
      { withCredentials: true },
    );
  }

  private toHotelApprovalRequest(request: StartProcessRequest): Record<string, any> {
    return {
      hotelId: request.businessKey,
      hostId: request.variables?.['hostId'],
      hotelName: request.variables?.['hotelName'],
      city: request.variables?.['city'],
      hostEmail: request.variables?.['hostEmail'],
    };
  }
}
